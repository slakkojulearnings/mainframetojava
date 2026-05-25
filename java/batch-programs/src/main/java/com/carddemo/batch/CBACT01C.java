package com.carddemo.batch;

import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.codec.TextCodec;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class CBACT01C {

    public static void main(String[] args) {
        Args parsed;
        try {
            parsed = Args.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(999);
            return;
        }
        int exitCode = new CBACT01C().run(parsed);
        System.exit(exitCode);
    }

    public int run(Args args) {
        try {
            System.out.println("START OF EXECUTION OF PROGRAM CBACT01C");

            FixedRecordWriter outWriter = FixedRecordWriter.open(args.outfile);
            FixedRecordWriter arryWriter = FixedRecordWriter.open(args.arryfile);
            FixedRecordWriter vbrcWriter = FixedRecordWriter.open(args.vbrcfile);

            try (FixedRecordReader acctReader = FixedRecordReader.open(args.acctfile, 300, FixedRecordReader.Mode.RAW)) {
                int acctCount = 0;
                byte[] raw;
                while ((raw = acctReader.nextRecord()) != null) {
                    acctCount++;
                    AccountRecord acct = AccountRecord.decode(raw);

                    displayAccountFields(acct);
                    writeOutFile(outWriter, acct);
                    writeArryFile(arryWriter, acct);
                    writeVbrcFile(vbrcWriter, acct);
                }

                System.out.println("ACCOUNT RECORDS PROCESSED: " + acctCount);
                System.out.println("END OF EXECUTION OF PROGRAM CBACT01C");

                outWriter.close();
                arryWriter.close();
                vbrcWriter.close();

                return 0;
            }

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 999;
        }
    }

    private void displayAccountFields(AccountRecord acct) {
        System.out.println("ACCT-ID                : " + acct.accountId());
        System.out.println("ACCT-ACTIVE-STATUS     : " + acct.activeStatus());
        System.out.println("ACCT-CURR-BAL          : " + acct.currBal());
        System.out.println("ACCT-CREDIT-LIMIT      : " + acct.creditLimit());
        System.out.println("ACCT-CASH-CREDIT-LIMIT : " + acct.cashCreditLimit());
        System.out.println("ACCT-OPEN-DATE         : " + acct.openDate());
        System.out.println("ACCT-EXPIRATION-DATE   : " + acct.expirationDate());
        System.out.println("ACCT-REISSUE-DATE      : " + acct.reissueDate());
        System.out.println("ACCT-CURR-CYC-CREDIT   : " + acct.currCycCredit());
        System.out.println("ACCT-CURR-CYC-DEBIT    : " + acct.currCycDebit());
        System.out.println("-------------------------------------------------");
    }

    private void writeOutFile(FixedRecordWriter writer, AccountRecord acct) throws IOException {
        byte[] record = new byte[100];

        // acctId (0, 11, zoned unsigned)
        byte[] acctIdEncoded = CbtrUtils.formatZonedDecimal(acct.accountId(), 11, false);
        System.arraycopy(acctIdEncoded, 0, record, 0, 11);

        // activeStatus (11, 1, text)
        record[11] = (byte) acct.activeStatus().charAt(0);

        // currBal (12, 12, zoned signed)
        byte[] currBalEncoded = CbtrUtils.formatZonedDecimal(acct.currBal().toPlainString(), 12, true);
        System.arraycopy(currBalEncoded, 0, record, 12, 12);

        // creditLimit (24, 12, zoned signed)
        byte[] creditLimitEncoded = CbtrUtils.formatZonedDecimal(acct.creditLimit().toPlainString(), 12, true);
        System.arraycopy(creditLimitEncoded, 0, record, 24, 12);

        // cashCreditLimit (36, 12, zoned signed)
        byte[] cashLimitEncoded = CbtrUtils.formatZonedDecimal(acct.cashCreditLimit().toPlainString(), 12, true);
        System.arraycopy(cashLimitEncoded, 0, record, 36, 12);

        // openDate (48, 10, text)
        byte[] openDateBytes = acct.openDate().getBytes();
        System.arraycopy(openDateBytes, 0, record, 48, Math.min(10, openDateBytes.length));

        // expirationDate (58, 10, text)
        byte[] expDateBytes = acct.expirationDate().getBytes();
        System.arraycopy(expDateBytes, 0, record, 58, Math.min(10, expDateBytes.length));

        // reissueDate reformatted (68, 10, text - first 8 chars as yyyyMMdd + 2 spaces)
        String yyyymmdd = LocalDate.parse(acct.reissueDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        byte[] reissueDateBytes = (yyyymmdd + "  ").getBytes();
        System.arraycopy(reissueDateBytes, 0, record, 68, 10);

        // currCycCredit (78, 12, zoned signed)
        byte[] currCycCreditEncoded = CbtrUtils.formatZonedDecimal(acct.currCycCredit().toPlainString(), 12, true);
        System.arraycopy(currCycCreditEncoded, 0, record, 78, 12);

        // currCycDebit (90, 7, zoned signed) - force to 2525.00 if zero
        BigDecimal cycDebit = acct.currCycDebit().compareTo(BigDecimal.ZERO) == 0
                ? new BigDecimal("2525.00")
                : acct.currCycDebit();
        byte[] cycDebitEncoded = CbtrUtils.formatZonedDecimal(cycDebit.toPlainString(), 7, true);
        System.arraycopy(cycDebitEncoded, 0, record, 90, 7);

        // groupId (97, 10, text)
        String groupId = acct.groupId() != null ? acct.groupId() : "";
        byte[] groupIdBytes = String.format("%-10s", groupId).getBytes();
        System.arraycopy(groupIdBytes, 0, record, 97, 10);

        writer.write(record);
    }

    private void writeArryFile(FixedRecordWriter writer, AccountRecord acct) throws IOException {
        byte[] record = new byte[115];

        // acctId (0, 11, zoned unsigned)
        byte[] acctIdEncoded = CbtrUtils.formatZonedDecimal(acct.accountId(), 11, false);
        System.arraycopy(acctIdEncoded, 0, record, 0, 11);

        int offset = 11;

        // Occurrence 1: currBal (actual) + cycDebit (1005.00)
        byte[] occ1Bal = CbtrUtils.formatZonedDecimal(acct.currBal().toPlainString(), 12, true);
        System.arraycopy(occ1Bal, 0, record, offset, 12);
        offset += 12;
        byte[] occ1Debit = CbtrUtils.formatZonedDecimal("1005.00", 12, true);
        System.arraycopy(occ1Debit, 0, record, offset, 12);
        offset += 12;

        // Occurrence 2: currBal (actual) + cycDebit (1525.00)
        byte[] occ2Bal = CbtrUtils.formatZonedDecimal(acct.currBal().toPlainString(), 12, true);
        System.arraycopy(occ2Bal, 0, record, offset, 12);
        offset += 12;
        byte[] occ2Debit = CbtrUtils.formatZonedDecimal("1525.00", 12, true);
        System.arraycopy(occ2Debit, 0, record, offset, 12);
        offset += 12;

        // Occurrence 3: currBal (-1025.00) + cycDebit (-2500.00)
        byte[] occ3Bal = CbtrUtils.formatZonedDecimal("-1025.00", 12, true);
        System.arraycopy(occ3Bal, 0, record, offset, 12);
        offset += 12;
        byte[] occ3Debit = CbtrUtils.formatZonedDecimal("-2500.00", 12, true);
        System.arraycopy(occ3Debit, 0, record, offset, 12);
        offset += 12;

        // Occurrences 4 and 5: zeroed (24 bytes each)
        byte[] zeroed = CbtrUtils.formatZonedDecimal("0.00", 12, true);
        System.arraycopy(zeroed, 0, record, offset, 12);
        offset += 12;
        System.arraycopy(zeroed, 0, record, offset, 12);
        offset += 12;
        System.arraycopy(zeroed, 0, record, offset, 12);
        offset += 12;
        System.arraycopy(zeroed, 0, record, offset, 12);
        offset += 12;

        // FILLER (4 bytes) - zeroed
        offset += 4;

        writer.write(record);
    }

    private void writeVbrcFile(FixedRecordWriter writer, AccountRecord acct) throws IOException {
        // VBR1: acctId (11, zoned unsigned) + activeStatus (1, text)
        byte[] vbr1 = new byte[12];
        byte[] acctIdEncoded = CbtrUtils.formatZonedDecimal(acct.accountId(), 11, false);
        System.arraycopy(acctIdEncoded, 0, vbr1, 0, 11);
        vbr1[11] = (byte) acct.activeStatus().charAt(0);
        writer.write(vbr1);

        // VBR2: acctId (11, zoned) + currBal (12, zoned signed) + creditLimit (12, zoned signed) + reissueYear (4, text)
        byte[] vbr2 = new byte[39];
        System.arraycopy(acctIdEncoded, 0, vbr2, 0, 11);

        byte[] currBalEncoded = CbtrUtils.formatZonedDecimal(acct.currBal().toPlainString(), 12, true);
        System.arraycopy(currBalEncoded, 0, vbr2, 11, 12);

        byte[] creditLimitEncoded = CbtrUtils.formatZonedDecimal(acct.creditLimit().toPlainString(), 12, true);
        System.arraycopy(creditLimitEncoded, 0, vbr2, 23, 12);

        String yyyymmdd = LocalDate.parse(acct.reissueDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String reissueYear = yyyymmdd.substring(0, 4);
        byte[] reissueYearBytes = reissueYear.getBytes();
        System.arraycopy(reissueYearBytes, 0, vbr2, 35, 4);

        writer.write(vbr2);
    }

    public static final class Args {
        public final Path acctfile, outfile, arryfile, vbrcfile;

        Args(Path acctfile, Path outfile, Path arryfile, Path vbrcfile) {
            this.acctfile = acctfile;
            this.outfile = outfile;
            this.arryfile = arryfile;
            this.vbrcfile = vbrcfile;
        }

        public static Args parse(String[] argv) {
            Path acctfile = null, outfile = null, arryfile = null, vbrcfile = null;

            for (int i = 0; i < argv.length; i++) {
                switch (argv[i]) {
                    case "--acctfile":
                        acctfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--outfile":
                        outfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--arryfile":
                        arryfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--vbrcfile":
                        vbrcfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                }
            }

            if (acctfile == null) throw new IllegalArgumentException("--acctfile required");
            if (outfile == null) throw new IllegalArgumentException("--outfile required");
            if (arryfile == null) throw new IllegalArgumentException("--arryfile required");
            if (vbrcfile == null) throw new IllegalArgumentException("--vbrcfile required");

            CbtrUtils.validateReadable(acctfile, "--acctfile");

            return new Args(acctfile, outfile, arryfile, vbrcfile);
        }
    }

    public CBACT01C() {}
}
