package com.carddemo.batch;

import com.carddemo.codec.Encoding;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CardRecord;
import com.carddemo.vsam.record.CustomerRecord;
import com.carddemo.vsam.record.DailyTransactionRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Java port of {@code app/cbl/CBTRN01C.cbl} — "Post transaction validation".
 *
 * <p>Diagnostic program that reads DALYTRAN records sequentially, looks up XREF by card number
 * and ACCOUNT by account ID. Displays transaction and lookup results to stdout. No writes.
 * Serves as a pre-run validator before CBTRN02C posting.
 */
public final class CBTRN01C {

    private static final String START_MSG = "START OF EXECUTION OF PROGRAM CBTRN01C";
    private static final String END_MSG = "END OF EXECUTION OF PROGRAM CBTRN01C";

    public static void main(String[] args) {
        Args parsed;
        try {
            parsed = Args.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(999);
            return;
        }
        int exitCode = new CBTRN01C().run(parsed);
        System.exit(exitCode);
    }

    public int run(Args args) {
        try {
            System.out.println(START_MSG);

            // Open all 6 files (custfile, cardfile, tranfile opened but not read — faithful to COBOL)
            KsdsReader xrefReader = KsdsReader.open(args.xreffile, 50, FixedRecordReader.Mode.RAW);
            KsdsReader acctReader = KsdsReader.open(args.acctfile, 300, FixedRecordReader.Mode.RAW);

            try (FixedRecordReader dalytranReader = FixedRecordReader.open(args.dalytran, 350, FixedRecordReader.Mode.RAW);
                 KsdsReader custReader = KsdsReader.open(args.custfile, 500, FixedRecordReader.Mode.RAW);
                 KsdsReader cardReader = KsdsReader.open(args.cardfile, 150, FixedRecordReader.Mode.RAW);
                 KsdsReader tranReader = KsdsReader.open(args.tranfile, 350, FixedRecordReader.Mode.RAW)) {

                byte[] raw;
                while ((raw = dalytranReader.nextRecord()) != null) {
                    DailyTransactionRecord tran = DailyTransactionRecord.decode(raw);
                    System.out.println("TRAN: " + tran.tranId() + " CARD: " + tran.cardNum());

                    // Look up XREF by card number
                    byte[] cardNumBytes = tran.cardNum().getBytes();
                    Optional<byte[]> xrefRaw = xrefReader.readByKey(cardNumBytes);
                    if (xrefRaw.isPresent()) {
                        com.carddemo.vsam.record.CardXrefRecord xref =
                            com.carddemo.vsam.record.CardXrefRecord.decode(xrefRaw.get());
                        System.out.println("  XREF FOUND: acctId=" + xref.accountId() + " custId=" + xref.custId());

                        // Look up ACCOUNT by account ID
                        byte[] acctKeyBytes = com.carddemo.batch.CbtrUtils.formatZonedDecimal(xref.accountId(), 11, false);
                        Optional<byte[]> acctRaw = acctReader.readByKey(acctKeyBytes);
                        if (acctRaw.isPresent()) {
                            AccountRecord acct = AccountRecord.decode(acctRaw.get());
                            System.out.println("  ACCOUNT FOUND: currBal=" + acct.currBal() + " status=" + acct.activeStatus());
                        } else {
                            System.out.println("  ACCOUNT NOT FOUND for acctId=" + xref.accountId());
                        }
                    } else {
                        System.out.println("  XREF NOT FOUND for card=" + tran.cardNum());
                    }
                }
            }

            xrefReader.close();
            acctReader.close();

            System.out.println(END_MSG);
            return 0;

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 999;
        }
    }

    public static final class Args {
        public final Path dalytran;
        public final Path custfile;
        public final Path xreffile;
        public final Path cardfile;
        public final Path acctfile;
        public final Path tranfile;
        public final Encoding encoding;

        Args(Path dalytran, Path custfile, Path xreffile, Path cardfile, Path acctfile,
             Path tranfile, Encoding encoding) {
            this.dalytran = dalytran;
            this.custfile = custfile;
            this.xreffile = xreffile;
            this.cardfile = cardfile;
            this.acctfile = acctfile;
            this.tranfile = tranfile;
            this.encoding = encoding;
        }

        public static Args parse(String[] argv) {
            Path dalytran = null, custfile = null, xreffile = null, cardfile = null,
                 acctfile = null, tranfile = null;
            Encoding encoding = Encoding.ASCII;

            for (int i = 0; i < argv.length; i++) {
                switch (argv[i]) {
                    case "--dalytran":
                        dalytran = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--custfile":
                        custfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--xreffile":
                        xreffile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--cardfile":
                        cardfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--acctfile":
                        acctfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--tranfile":
                        tranfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--encoding":
                        encoding = Encoding.valueOf(CbtrUtils.requireNext(argv, i++).toUpperCase());
                        break;
                    case "-h":
                    case "--help":
                        printHelp();
                        System.exit(0);
                        break;
                }
            }

            if (dalytran == null) throw new IllegalArgumentException("--dalytran required");
            if (custfile == null) throw new IllegalArgumentException("--custfile required");
            if (xreffile == null) throw new IllegalArgumentException("--xreffile required");
            if (cardfile == null) throw new IllegalArgumentException("--cardfile required");
            if (acctfile == null) throw new IllegalArgumentException("--acctfile required");
            if (tranfile == null) throw new IllegalArgumentException("--tranfile required");

            CbtrUtils.validateReadable(dalytran, "--dalytran");
            CbtrUtils.validateReadable(custfile, "--custfile");
            CbtrUtils.validateReadable(xreffile, "--xreffile");
            CbtrUtils.validateReadable(cardfile, "--cardfile");
            CbtrUtils.validateReadable(acctfile, "--acctfile");
            CbtrUtils.validateReadable(tranfile, "--tranfile");

            return new Args(dalytran, custfile, xreffile, cardfile, acctfile, tranfile, encoding);
        }

        private static void printHelp() {
            System.out.println("CBTRN01C — transaction validation diagnostic");
            System.out.println();
            System.out.println("Usage:");
            System.out.println("  java com.carddemo.batch.CBTRN01C \\");
            System.out.println("       --dalytran <path>  [required] daily transactions");
            System.out.println("       --custfile  <path>  [required] customer master");
            System.out.println("       --xreffile  <path>  [required] card cross-ref");
            System.out.println("       --cardfile  <path>  [required] card master");
            System.out.println("       --acctfile  <path>  [required] account master");
            System.out.println("       --tranfile  <path>  [required] transaction file");
            System.out.println("       --encoding  ASCII|EBCDIC  [default ASCII]");
        }
    }

    public CBTRN01C() {}
}
