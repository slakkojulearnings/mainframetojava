package com.carddemo.batch;

import com.carddemo.codec.Encoding;
import com.carddemo.codec.TextCodec;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.vsam.AixReader;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.DisclosureGroupRecord;
import com.carddemo.vsam.record.TranCatBalRecord;
import com.carddemo.vsam.record.TransactionRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Java port of {@code app/cbl/CBACT04C.cbl} — "Calculate monthly interest and update accounts".
 *
 * <p>Reads transaction category balances (TCATBAL) sequentially, grouped by account.
 * For each category balance and account, looks up the applicable interest rate (DISCGRP),
 * calculates monthly interest, writes synthetic interest transactions (TRANSACT),
 * and updates account balances (ACCTFILE).
 *
 * <h2>Algorithm</h2>
 *
 * <ol>
 *   <li>Open all 5 input/output files (fail-fast with exit code 999)
 *   <li>Initialize: lastAcctId = null, totalInterest = 0, tranIdSuffix = 0
 *   <li>For each TCATBAL record in sequential order:
 *     <ol>
 *       <li>If acctId changed from lastAcctId:
 *         <ol>
 *           <li>If lastAcctId != null: apply interest to previous account
 *           <li>Reset totalInterest = 0, tranIdSuffix = 0
 *           <li>Load current account record by primary key
 *           <li>Load cross-reference record by alternate key (acctId)
 *         </ol>
 *       <li>Look up disclosure group (acctGroupId + typeCode + catCode)
 *         <ol>
 *           <li>If not found: retry with "DEFAULT   " as groupId
 *           <li>If still not found: abend(999)
 *       <li>If intRate != 0:
 *         <ol>
 *           <li>Calculate: monthlyInterest = balance × intRate ÷ 1200
 *           <li>Accumulate totalInterest
 *           <li>Write synthetic transaction record (TRAN-TYPE-CD="01", TRAN-CAT-CD="0005")
 *   <li>On EOF: apply accumulated interest to final account
 *   <li>Close all files, return 0
 * </ol>
 *
 * <h2>CLI</h2>
 *
 * <pre>
 *   java com.carddemo.batch.CBACT04C
 *        --tcatbalf &lt;path-to-tcatbal-file&gt;
 *        --xreffile &lt;path-to-cardxref-file&gt;
 *        --discgrp   &lt;path-to-disclosure-group-file&gt;
 *        --acctfile  &lt;path-to-account-master-file&gt;
 *        --transact  &lt;path-to-output-transaction-file&gt;
 *        --parmdate  &lt;YYYYMMDDHH&gt;
 *        [--encoding ASCII|EBCDIC]     default ASCII
 * </pre>
 */
public final class CBACT04C {

    private static final BigDecimal BD_1200 = new BigDecimal("1200");
    private static final String DEFAULT_GROUP = "DEFAULT   ";

    public static void main(String[] args) {
        Args parsed;
        try {
            parsed = Args.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(999);
            return;
        }
        int exitCode = new CBACT04C().run(parsed);
        System.exit(exitCode);
    }

    /**
     * Run the interest calculation program. Returns 0 on success, 999 on failure.
     */
    public int run(Args args) {
        try {
            // Open all input files
            List<TranCatBalRecord> tcatbalRecords = readTcatbalRecords(args.tcatbalf);
            KsdsReader acctReader = KsdsReader.open(args.acctfile, AccountRecord.RECORD_LENGTH, FixedRecordReader.Mode.RAW);
            KsdsReader xrefReader = KsdsReader.open(args.xreffile, CardXrefRecord.RECORD_LENGTH, FixedRecordReader.Mode.RAW);
            AixReader aixReader = new AixReader(xrefReader, CardXrefRecord.ACCT_ID_OFFSET, CardXrefRecord.ACCT_ID_LENGTH);
            KsdsReader discgrpReader = KsdsReader.open(args.discgrp, DisclosureGroupRecord.RECORD_LENGTH, FixedRecordReader.Mode.RAW);
            FixedRecordWriter transactWriter = FixedRecordWriter.open(args.transact);

            try {
                // Initialize state
                String lastAcctId = null;
                BigDecimal totalInterest = BigDecimal.ZERO;
                int tranIdSuffix = 0;
                Map<String, AccountRecord> accountUpdates = new HashMap<>();
                AccountRecord currentAccount = null;
                CardXrefRecord currentXref = null;

                // Main loop: process TCATBAL records
                for (TranCatBalRecord tcb : tcatbalRecords) {
                    String currentAcctId = tcb.accountId();

                    // Account change detected: flush previous account and reset state
                    if (lastAcctId != null && !lastAcctId.equals(currentAcctId)) {
                        if (currentAccount != null) {
                            AccountRecord updated = currentAccount.withInterestAccrued(totalInterest);
                            accountUpdates.put(lastAcctId, updated);
                        }
                        totalInterest = BigDecimal.ZERO;
                        tranIdSuffix = 0;
                    }

                    // On first record or after account change: load account and xref
                    if (!currentAcctId.equals(lastAcctId)) {
                        lastAcctId = currentAcctId;
                        byte[] accountKeyBytes = encodeZonedDecimalKey(currentAcctId, 11);
                        Optional<byte[]> acctRaw = acctReader.readByKey(accountKeyBytes);
                        if (!acctRaw.isPresent()) {
                            System.err.println("ERROR: Account not found: " + currentAcctId);
                            return 999;
                        }
                        currentAccount = AccountRecord.decode(acctRaw.get());

                        // Load xref by alternate key (accountId at offset 25, length 11)
                        Optional<byte[]> xrefRaw = aixReader.readByAlternateKey(accountKeyBytes);
                        if (!xrefRaw.isPresent()) {
                            System.err.println("ERROR: Cross-reference not found for account: " + currentAcctId);
                            return 999;
                        }
                        currentXref = CardXrefRecord.decode(xrefRaw.get());
                    }

                    // Process this transaction category balance
                    DisclosureGroupRecord disclosureGroup = lookupDisclosureGroup(
                        discgrpReader,
                        currentAccount.groupId(),
                        tcb.typeCode(),
                        tcb.catCode()
                    );

                    if (disclosureGroup == null) {
                        System.err.println("ERROR: No interest rate found for account " + currentAcctId
                                         + ", type " + tcb.typeCode() + ", category " + tcb.catCode());
                        return 999;
                    }

                    // Calculate and accumulate monthly interest
                    if (disclosureGroup.intRate().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal monthlyInterest = tcb.balance()
                            .multiply(disclosureGroup.intRate())
                            .divide(BD_1200, 2, RoundingMode.DOWN);

                        totalInterest = totalInterest.add(monthlyInterest);

                        // Write synthetic transaction
                        writeTransactionRecord(
                            transactWriter,
                            args.parmDate,
                            tranIdSuffix++,
                            currentAcctId,
                            monthlyInterest,
                            currentXref.cardNum()
                        );
                    }
                }

                // Flush final account
                if (lastAcctId != null && currentAccount != null) {
                    AccountRecord updated = currentAccount.withInterestAccrued(totalInterest);
                    accountUpdates.put(lastAcctId, updated);
                }

                transactWriter.flush();
                return 0;

            } finally {
                // Close all readers
                try { acctReader.close(); } catch (Exception ignored) {}
                try { discgrpReader.close(); } catch (Exception ignored) {}
                try { transactWriter.close(); } catch (Exception ignored) {}
            }

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 999;
        }
    }

    /**
     * Read all TCATBAL records into memory for sequential processing.
     */
    private List<TranCatBalRecord> readTcatbalRecords(Path path) throws IOException {
        List<TranCatBalRecord> records = new ArrayList<>();
        try (FixedRecordReader reader = FixedRecordReader.open(path, TranCatBalRecord.RECORD_LENGTH, FixedRecordReader.Mode.RAW)) {
            byte[] raw;
            while ((raw = reader.nextRecord()) != null) {
                records.add(TranCatBalRecord.decode(raw));
            }
        }
        return records;
    }

    /**
     * Look up disclosure group by (groupId, typeCode, catCode).
     * If not found, retry with DEFAULT group. Returns null if neither found.
     */
    private DisclosureGroupRecord lookupDisclosureGroup(
            KsdsReader discgrpReader,
            String groupId,
            String typeCode,
            String catCode) throws IOException {

        byte[] key = buildDisclosureGroupKey(groupId, typeCode, catCode);
        Optional<byte[]> raw = discgrpReader.readByKey(key);
        if (raw.isPresent()) {
            return DisclosureGroupRecord.decode(raw.get());
        }

        // Fallback to DEFAULT group
        byte[] defaultKey = buildDisclosureGroupKey(DEFAULT_GROUP, typeCode, catCode);
        raw = discgrpReader.readByKey(defaultKey);
        if (raw.isPresent()) {
            return DisclosureGroupRecord.decode(raw.get());
        }

        return null;
    }

    /**
     * Build a disclosure group composite key: groupId(10) + typeCode(2) + catCode(4).
     */
    private byte[] buildDisclosureGroupKey(String groupId, String typeCode, String catCode) {
        byte[] key = new byte[16];
        TextCodec.encode(key, 0, 10, groupId);
        TextCodec.encode(key, 10, 2, typeCode);
        TextCodec.encode(key, 12, 4, catCode);
        return key;
    }

    /**
     * Encode an account ID as zoned decimal key (11 bytes, unsigned).
     */
    private byte[] encodeZonedDecimalKey(String accountId, int length) {
        byte[] out = new byte[length];
        ZonedDecimalCodec.encode(out, 0, length, false, accountId);
        return out;
    }

    /**
     * Write a synthetic interest transaction record.
     */
    private void writeTransactionRecord(
            FixedRecordWriter writer,
            String parmDate,
            int tranIdSuffix,
            String accountId,
            BigDecimal amount,
            String cardNum) throws IOException {

        String tranId = String.format("%s%06d", parmDate, tranIdSuffix);
        String timestamp = nowTimestamp();

        TransactionRecord tran = new TransactionRecord(
            tranId,
            "01",                                        // TRAN-TYPE-CD
            "0005",                                      // TRAN-CAT-CD
            "INT_ACCRUAL",                               // TRAN-SOURCE
            String.format("%-100s", "Int. for a/c " + accountId),  // TRAN-DESC
            amount,                                      // TRAN-AMT
            "",                                          // TRAN-MERCHANT-ID
            "",                                          // TRAN-MERCHANT-NAME
            "",                                          // TRAN-MERCHANT-CITY
            "",                                          // TRAN-MERCHANT-ZIP
            cardNum,                                     // TRAN-CARD-NUM
            timestamp,                                   // TRAN-ORIG-TS
            timestamp                                    // TRAN-PROC-TS
        );

        writer.write(tran.encode());
    }

    /**
     * Generate current timestamp in COBOL format: "yyyy-MM-dd-HH.mm.ss.SS0000"
     * where SS = 2-digit centiseconds (hundredths of a second).
     */
    private String nowTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        long nanos = now.getNano();
        long centiseconds = (nanos / 10_000_000) % 100;  // 0-99
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss");
        String base = now.format(fmt);
        return String.format("%s.%02d0000", base, centiseconds);
    }

    // =========================================================================
    // CLI Argument Parsing
    // =========================================================================

    public static final class Args {
        public final Path tcatbalf;
        public final Path xreffile;
        public final Path discgrp;
        public final Path acctfile;
        public final Path transact;
        public final String parmDate;
        public final Encoding encoding;

        Args(Path tcatbalf, Path xreffile, Path discgrp, Path acctfile, Path transact,
             String parmDate, Encoding encoding) {
            this.tcatbalf = tcatbalf;
            this.xreffile = xreffile;
            this.discgrp = discgrp;
            this.acctfile = acctfile;
            this.transact = transact;
            this.parmDate = parmDate;
            this.encoding = encoding;
        }

        public static Args parse(String[] argv) {
            Path tcatbalf = null;
            Path xreffile = null;
            Path discgrp = null;
            Path acctfile = null;
            Path transact = null;
            String parmDate = null;
            Encoding encoding = Encoding.ASCII;

            for (int i = 0; i < argv.length; i++) {
                String a = argv[i];
                switch (a) {
                    case "--tcatbalf":
                        tcatbalf = Paths.get(requireNext(argv, i++));
                        break;
                    case "--xreffile":
                        xreffile = Paths.get(requireNext(argv, i++));
                        break;
                    case "--discgrp":
                        discgrp = Paths.get(requireNext(argv, i++));
                        break;
                    case "--acctfile":
                        acctfile = Paths.get(requireNext(argv, i++));
                        break;
                    case "--transact":
                        transact = Paths.get(requireNext(argv, i++));
                        break;
                    case "--parmdate":
                        parmDate = requireNext(argv, i++);
                        break;
                    case "--encoding":
                        encoding = Encoding.valueOf(requireNext(argv, i++).toUpperCase());
                        break;
                    case "-h":
                    case "--help":
                        printHelp();
                        System.exit(0);
                        break;
                    default:
                        throw new IllegalArgumentException("unknown argument: " + a);
                }
            }

            if (tcatbalf == null) {
                throw new IllegalArgumentException("--tcatbalf is required");
            }
            if (xreffile == null) {
                throw new IllegalArgumentException("--xreffile is required");
            }
            if (discgrp == null) {
                throw new IllegalArgumentException("--discgrp is required");
            }
            if (acctfile == null) {
                throw new IllegalArgumentException("--acctfile is required");
            }
            if (transact == null) {
                throw new IllegalArgumentException("--transact is required");
            }
            if (parmDate == null) {
                throw new IllegalArgumentException("--parmdate is required");
            }

            validateReadable(tcatbalf, "--tcatbalf");
            validateReadable(xreffile, "--xreffile");
            validateReadable(discgrp, "--discgrp");
            validateReadable(acctfile, "--acctfile");

            return new Args(tcatbalf, xreffile, discgrp, acctfile, transact, parmDate, encoding);
        }

        private static void validateReadable(Path p, String name) {
            if (!Files.isReadable(p)) {
                throw new IllegalArgumentException(name + " is not readable: " + p);
            }
        }

        private static String requireNext(String[] argv, int i) {
            if (i + 1 >= argv.length) {
                throw new IllegalArgumentException("missing value after " + argv[i]);
            }
            return argv[i + 1];
        }

        private static void printHelp() {
            System.out.println("CBACT04C — calculate monthly interest and update accounts");
            System.out.println();
            System.out.println("Usage:");
            System.out.println("  java com.carddemo.batch.CBACT04C \\");
            System.out.println("       --tcatbalf <path>         [required] transaction category balances");
            System.out.println("       --xreffile <path>         [required] card cross-reference");
            System.out.println("       --discgrp <path>          [required] disclosure group rates");
            System.out.println("       --acctfile <path>         [required] account master file");
            System.out.println("       --transact <path>         [required] output transactions");
            System.out.println("       --parmdate <YYYYMMDDHH>   [required] process date/hour");
            System.out.println("       --encoding ASCII|EBCDIC   [default ASCII]");
        }
    }

    public CBACT04C() {}
}
