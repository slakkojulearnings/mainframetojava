package com.carddemo.batch;

import com.carddemo.codec.Encoding;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.KsdsStore;
import com.carddemo.vsam.record.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class CBTRN02C {

    private static final BigDecimal BD_1200 = new BigDecimal("1200");

    public static void main(String[] args) {
        Args parsed;
        try {
            parsed = Args.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(999);
            return;
        }
        int exitCode = new CBTRN02C().run(parsed);
        System.exit(exitCode);
    }

    public int run(Args args) {
        try {
            System.out.println("START OF EXECUTION OF PROGRAM CBTRN02C");

            // Open readers
            KsdsReader xrefReader = KsdsReader.open(args.xreffile, 50, FixedRecordReader.Mode.RAW);
            KsdsStore acctStore = KsdsStore.open(args.acctfile, 300, 11, FixedRecordReader.Mode.RAW);
            KsdsStore tcatbalStore = KsdsStore.open(args.tcatbalf, 50, 17, FixedRecordReader.Mode.RAW);

            // Open writers
            FixedRecordWriter transactWriter = FixedRecordWriter.open(args.transact);
            FixedRecordWriter rejectWriter = FixedRecordWriter.open(args.dalyrejs);

            try (FixedRecordReader dalytranReader = FixedRecordReader.open(args.dalytran, 350, FixedRecordReader.Mode.RAW)) {
                int tranCount = 0, rejectCount = 0;

                byte[] raw;
                while ((raw = dalytranReader.nextRecord()) != null) {
                    tranCount++;
                    DailyTransactionRecord tran = DailyTransactionRecord.decode(raw);

                    ValidationResult v = validate(tran, xrefReader, acctStore);
                    if (v.valid) {
                        post(tran, v.xref.get(), v.acct.get(), acctStore, tcatbalStore, transactWriter);
                    } else {
                        rejectCount++;
                        writeReject(tran, v.code, v.desc, rejectWriter);
                    }
                }

                // Flush updated files
                acctStore.flush(args.acctfile);
                tcatbalStore.flush(args.tcatbalf);

                System.out.println("RECORDS PROCESSED: " + tranCount + "  RECORDS REJECTED: " + rejectCount);
                System.out.println("END OF EXECUTION OF PROGRAM CBTRN02C");

                transactWriter.close();
                rejectWriter.close();
                xrefReader.close();
                acctStore.close();
                tcatbalStore.close();

                return rejectCount > 0 ? 4 : 0;
            }

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 999;
        }
    }

    private ValidationResult validate(DailyTransactionRecord tran, KsdsReader xrefReader, KsdsStore acctStore)
            throws IOException {
        // Step 1: XREF lookup
        byte[] cardNumBytes = tran.cardNum().getBytes();
        Optional<byte[]> xrefRaw = xrefReader.readByKey(cardNumBytes);
        if (!xrefRaw.isPresent()) {
            return new ValidationResult(100, "INVALID CARD NUMBER FOUND");
        }
        CardXrefRecord xref = CardXrefRecord.decode(xrefRaw.get());

        // Step 2: ACCOUNT lookup
        byte[] acctKeyBytes = CbtrUtils.formatZonedDecimal(xref.accountId(), 11, false);
        Optional<byte[]> acctRaw = acctStore.readByKey(acctKeyBytes);
        if (!acctRaw.isPresent()) {
            return new ValidationResult(101, "ACCOUNT RECORD NOT FOUND");
        }
        AccountRecord acct = AccountRecord.decode(acctRaw.get());

        // Step 3: Overlimit check
        BigDecimal tempBal = acct.currCycCredit()
            .subtract(acct.currCycDebit())
            .add(tran.amount());
        if (acct.creditLimit().compareTo(tempBal) < 0) {
            return new ValidationResult(102, "OVERLIMIT TRANSACTION", Optional.of(xref), Optional.of(acct));
        }

        // Step 4: Expiry check — can overwrite 102
        String tranDate = tran.origTs().substring(0, 10);
        if (tranDate.compareTo(acct.expirationDate()) > 0) {
            return new ValidationResult(103, "TRANSACTION RECEIVED AFTER ACCT EXPIRATION", Optional.of(xref), Optional.of(acct));
        }

        return new ValidationResult(Optional.of(xref), Optional.of(acct));
    }

    private void post(DailyTransactionRecord tran, CardXrefRecord xref, AccountRecord acct,
                      KsdsStore acctStore, KsdsStore tcatbalStore, FixedRecordWriter transactWriter)
            throws IOException {
        // Write transaction
        TransactionRecord t = new TransactionRecord(
            tran.tranId(), tran.typeCode(), tran.catCode(), tran.source(),
            String.format("%-100s", tran.desc()), tran.amount(),
            tran.merchantId(), tran.merchantName(), tran.merchantCity(),
            tran.merchantZip(), tran.cardNum(), tran.origTs(), nowTimestamp()
        );
        transactWriter.write(t.encode());

        // Update TCATBAL
        byte[] tcatKey = buildTcatKey(xref.accountId(), tran.typeCode(), tran.catCode());
        Optional<byte[]> tcatRaw = tcatbalStore.readByKey(tcatKey);
        TranCatBalRecord tcatbal;
        if (tcatRaw.isPresent()) {
            tcatbal = TranCatBalRecord.decode(tcatRaw.get()).withAddedAmount(tran.amount());
        } else {
            tcatbal = new TranCatBalRecord(xref.accountId(), tran.typeCode(), tran.catCode(), tran.amount(), null);
        }
        tcatbalStore.write(tcatbal.encode());

        // Update Account
        BigDecimal newBal = acct.currBal().add(tran.amount());
        BigDecimal newCredit = tran.amount().compareTo(BigDecimal.ZERO) >= 0
            ? acct.currCycCredit().add(tran.amount()) : acct.currCycCredit();
        BigDecimal newDebit = tran.amount().compareTo(BigDecimal.ZERO) < 0
            ? acct.currCycDebit().add(tran.amount()) : acct.currCycDebit();

        AccountRecord updated = new AccountRecord(
            acct.accountId(), acct.activeStatus(), newBal, acct.creditLimit(),
            acct.cashCreditLimit(), acct.openDate(), acct.expirationDate(),
            acct.reissueDate(), newCredit, newDebit, acct.addrZip(), acct.groupId(), null
        );
        acctStore.write(updated.encode());
    }

    private void writeReject(DailyTransactionRecord tran, int code, String desc, FixedRecordWriter writer)
            throws IOException {
        byte[] reject = new byte[430];
        System.arraycopy(tran.raw(), 0, reject, 0, 350);
        String codeStr = String.format("%04d", code);
        System.arraycopy(codeStr.getBytes(), 0, reject, 350, 4);
        String descPad = String.format("%-76s", desc);
        System.arraycopy(descPad.getBytes(), 0, reject, 354, 76);
        writer.write(reject);
    }

    private byte[] buildTcatKey(String acctId, String typeCode, String catCode) {
        byte[] key = new byte[17];
        CbtrUtils.formatZonedDecimal(acctId, 11, false);
        System.arraycopy(CbtrUtils.formatZonedDecimal(acctId, 11, false), 0, key, 0, 11);
        System.arraycopy(typeCode.getBytes(), 0, key, 11, 2);
        System.arraycopy(CbtrUtils.formatZonedDecimal(catCode, 4, false), 0, key, 13, 4);
        return key;
    }

    private String nowTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        long nanos = now.getNano();
        long centis = (nanos / 10_000_000) % 100;
        String base = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss"));
        return String.format("%s.%02d0000", base, centis);
    }

    private static class ValidationResult {
        final boolean valid;
        final Optional<CardXrefRecord> xref;
        final Optional<AccountRecord> acct;
        final int code;
        final String desc;

        ValidationResult(Optional<CardXrefRecord> xref, Optional<AccountRecord> acct) {
            this.valid = true;
            this.xref = xref;
            this.acct = acct;
            this.code = 0;
            this.desc = "";
        }

        ValidationResult(int code, String desc) {
            this.valid = false;
            this.xref = Optional.empty();
            this.acct = Optional.empty();
            this.code = code;
            this.desc = desc;
        }

        ValidationResult(int code, String desc, Optional<CardXrefRecord> xref, Optional<AccountRecord> acct) {
            this.valid = false;
            this.xref = xref;
            this.acct = acct;
            this.code = code;
            this.desc = desc;
        }
    }

    public static final class Args {
        public final Path dalytran, xreffile, acctfile, tcatbalf, transact, dalyrejs;

        Args(Path dalytran, Path xreffile, Path acctfile, Path tcatbalf, Path transact, Path dalyrejs) {
            this.dalytran = dalytran;
            this.xreffile = xreffile;
            this.acctfile = acctfile;
            this.tcatbalf = tcatbalf;
            this.transact = transact;
            this.dalyrejs = dalyrejs;
        }

        public static Args parse(String[] argv) {
            Path dalytran = null, xreffile = null, acctfile = null, tcatbalf = null, transact = null, dalyrejs = null;

            for (int i = 0; i < argv.length; i++) {
                switch (argv[i]) {
                    case "--dalytran":
                        dalytran = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--xreffile":
                        xreffile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--acctfile":
                        acctfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--tcatbalf":
                        tcatbalf = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--transact":
                        transact = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--dalyrejs":
                        dalyrejs = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                }
            }

            if (dalytran == null) throw new IllegalArgumentException("--dalytran required");
            if (xreffile == null) throw new IllegalArgumentException("--xreffile required");
            if (acctfile == null) throw new IllegalArgumentException("--acctfile required");
            if (tcatbalf == null) throw new IllegalArgumentException("--tcatbalf required");
            if (transact == null) throw new IllegalArgumentException("--transact required");
            if (dalyrejs == null) throw new IllegalArgumentException("--dalyrejs required");

            CbtrUtils.validateReadable(dalytran, "--dalytran");
            CbtrUtils.validateReadable(xreffile, "--xreffile");
            CbtrUtils.validateReadable(acctfile, "--acctfile");
            CbtrUtils.validateReadable(tcatbalf, "--tcatbalf");

            return new Args(dalytran, xreffile, acctfile, tcatbalf, transact, dalyrejs);
        }
    }

    public CBTRN02C() {}
}
