package com.carddemo.batch;

import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.TranCatRecord;
import com.carddemo.vsam.record.TranTypeRecord;
import com.carddemo.vsam.record.TransactionRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public final class CBTRN03C {

    public static void main(String[] args) {
        Args parsed;
        try {
            parsed = Args.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(999);
            return;
        }
        int exitCode = new CBTRN03C().run(parsed);
        System.exit(exitCode);
    }

    public int run(Args args) {
        try {
            System.out.println("START OF EXECUTION OF PROGRAM CBTRN03C");

            KsdsReader xrefReader = KsdsReader.open(args.xreffile, 50, FixedRecordReader.Mode.RAW);
            KsdsReader tranTypeReader = KsdsReader.open(args.trantype, 60, FixedRecordReader.Mode.RAW);
            KsdsReader tranCatReader = KsdsReader.open(args.trancatg, 60, FixedRecordReader.Mode.RAW);

            FixedRecordWriter reportWriter = FixedRecordWriter.open(args.tranrept);

            try (FixedRecordReader dateParamReader = FixedRecordReader.open(args.dateparm, 80, FixedRecordReader.Mode.RAW);
                 FixedRecordReader tranReader = FixedRecordReader.open(args.tranfile, 350, FixedRecordReader.Mode.RAW)) {

                byte[] dateParamRaw = dateParamReader.nextRecord();
                if (dateParamRaw == null) {
                    System.err.println("ERROR: DATEPARM file is empty");
                    return 999;
                }
                String startDate = new String(dateParamRaw, 0, 10).trim();
                String endDate = new String(dateParamRaw, 11, 10).trim();

                writeReportHeader(reportWriter, startDate, endDate);
                writeColumnHeaders(reportWriter);

                String prevCardNum = null;
                String currentAcctId = "";
                BigDecimal pageTotal = BigDecimal.ZERO;
                BigDecimal acctTotal = BigDecimal.ZERO;
                BigDecimal grandTotal = BigDecimal.ZERO;
                int lineCount = 0;
                int tranCount = 0;

                byte[] raw;
                while ((raw = tranReader.nextRecord()) != null) {
                    TransactionRecord tran = TransactionRecord.decode(raw);
                    tranCount++;

                    String tranDate = tran.procTs().substring(0, 10);
                    if (tranDate.compareTo(startDate) < 0 || tranDate.compareTo(endDate) > 0) {
                        continue;
                    }

                    String currentCardNum = tran.cardNum().trim();
                    if (prevCardNum == null || !prevCardNum.equals(currentCardNum)) {
                        if (prevCardNum != null) {
                            writeAccountTotal(reportWriter, acctTotal);
                            lineCount++;
                            if (lineCount % 20 == 0) {
                                writePageTotal(reportWriter, pageTotal);
                                grandTotal = grandTotal.add(pageTotal);
                                pageTotal = BigDecimal.ZERO;
                                writeColumnHeaders(reportWriter);
                                lineCount = 0;
                            }
                        }

                        Optional<byte[]> xrefRaw = xrefReader.readByKey(currentCardNum.getBytes());
                        if (!xrefRaw.isPresent()) {
                            System.err.println("ERROR: XREF not found for card=" + currentCardNum);
                            return 999;
                        }
                        CardXrefRecord xref = CardXrefRecord.decode(xrefRaw.get());
                        currentAcctId = xref.accountId();
                        prevCardNum = currentCardNum;
                        acctTotal = BigDecimal.ZERO;
                    }

                    Optional<byte[]> typeRaw = tranTypeReader.readByKey(tran.typeCode().getBytes());
                    String typeDesc = "";
                    if (typeRaw.isPresent()) {
                        TranTypeRecord typeRec = TranTypeRecord.decode(typeRaw.get());
                        typeDesc = typeRec.typeDesc();
                    }

                    byte[] catKey = buildCatKey(tran.typeCode(), tran.catCode());
                    Optional<byte[]> catRaw = tranCatReader.readByKey(catKey);
                    String catDesc = "";
                    if (catRaw.isPresent()) {
                        TranCatRecord catRec = TranCatRecord.decode(catRaw.get());
                        catDesc = catRec.catDesc();
                    }

                    pageTotal = pageTotal.add(tran.amount());
                    acctTotal = acctTotal.add(tran.amount());
                    writeDetailLine(reportWriter, tran, currentAcctId, typeDesc, catDesc);
                    lineCount++;

                    if (lineCount % 20 == 0) {
                        writePageTotal(reportWriter, pageTotal);
                        grandTotal = grandTotal.add(pageTotal);
                        pageTotal = BigDecimal.ZERO;
                        writeColumnHeaders(reportWriter);
                        lineCount = 0;
                    }
                }

                if (prevCardNum != null) {
                    writeAccountTotal(reportWriter, acctTotal);
                }
                if (pageTotal.compareTo(BigDecimal.ZERO) != 0) {
                    writePageTotal(reportWriter, pageTotal);
                    grandTotal = grandTotal.add(pageTotal);
                }
                writeGrandTotal(reportWriter, grandTotal);

                System.out.println("RECORDS PROCESSED: " + tranCount);
                System.out.println("END OF EXECUTION OF PROGRAM CBTRN03C");

                reportWriter.close();
                xrefReader.close();
                tranTypeReader.close();
                tranCatReader.close();

                return 0;
            }

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 999;
        }
    }

    private byte[] buildCatKey(String typeCode, String catCode) {
        byte[] key = new byte[6];
        System.arraycopy(typeCode.getBytes(), 0, key, 0, 2);
        System.arraycopy(CbtrUtils.formatZonedDecimal(catCode, 4, false), 0, key, 2, 4);
        return key;
    }

    private void writeReportHeader(FixedRecordWriter writer, String startDate, String endDate) throws IOException {
        String header = String.format("%-38s%-41s%-133s", "DALYREPT",
                "Daily Transaction Report", "Date Range: " + startDate + " to " + endDate);
        byte[] line = new byte[133];
        String padded = String.format("%-133s", header);
        for (int i = 0; i < 133 && i < padded.length(); i++) {
            line[i] = (byte) padded.charAt(i);
        }
        writer.write(line);
    }

    private void writeColumnHeaders(FixedRecordWriter writer) throws IOException {
        String header = "Transaction ID   Account ID     Transaction Type     Tran Category                       Tran Source           Amount";
        byte[] line = new byte[133];
        String padded = String.format("%-133s", header);
        for (int i = 0; i < 133 && i < padded.length(); i++) {
            line[i] = (byte) padded.charAt(i);
        }
        writer.write(line);

        byte[] separator = new byte[133];
        for (int i = 0; i < 133; i++) {
            separator[i] = (byte) '-';
        }
        writer.write(separator);
    }

    private void writeDetailLine(FixedRecordWriter writer, TransactionRecord tran, String acctId,
                                 String typeDesc, String catDesc) throws IOException {
        String tranId = String.format("%-16s", tran.tranId());
        String acctIdStr = String.format("%-11s", acctId);
        String typeCodePart = tran.typeCode() + "-" + truncate(typeDesc, 15);
        String catCodePart = truncate(tran.catCode(), 4) + "-" + truncate(catDesc, 29);
        String source = String.format("%-10s", tran.source());
        String amount = formatAmount(tran.amount());

        String detail = tranId + " " + acctIdStr + " " + typeCodePart + " " + catCodePart + " " + source + "    " + amount;
        byte[] line = new byte[133];
        String padded = String.format("%-133s", detail);
        for (int i = 0; i < 133 && i < padded.length(); i++) {
            line[i] = (byte) padded.charAt(i);
        }
        writer.write(line);
    }

    private void writePageTotal(FixedRecordWriter writer, BigDecimal amount) throws IOException {
        String detail = "Page Total    " + dots(86) + formatAmount(amount);
        byte[] line = new byte[133];
        String padded = String.format("%-133s", detail);
        for (int i = 0; i < 133 && i < padded.length(); i++) {
            line[i] = (byte) padded.charAt(i);
        }
        writer.write(line);
    }

    private void writeAccountTotal(FixedRecordWriter writer, BigDecimal amount) throws IOException {
        String detail = "Account Total " + dots(84) + formatAmount(amount);
        byte[] line = new byte[133];
        String padded = String.format("%-133s", detail);
        for (int i = 0; i < 133 && i < padded.length(); i++) {
            line[i] = (byte) padded.charAt(i);
        }
        writer.write(line);
    }

    private void writeGrandTotal(FixedRecordWriter writer, BigDecimal amount) throws IOException {
        String detail = "Grand Total   " + dots(86) + formatAmount(amount);
        byte[] line = new byte[133];
        String padded = String.format("%-133s", detail);
        for (int i = 0; i < 133 && i < padded.length(); i++) {
            line[i] = (byte) padded.charAt(i);
        }
        writer.write(line);
    }

    private String formatAmount(BigDecimal amount) {
        String formatted = String.format("%,15.2f", amount);
        return formatted.substring(Math.max(0, formatted.length() - 15));
    }

    private String dots(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append('.');
        }
        return sb.toString();
    }

    private String truncate(String value, int length) {
        if (value == null || value.length() <= length) {
            return String.format("%-" + length + "s", value != null ? value : "");
        }
        return value.substring(0, length);
    }

    public static final class Args {
        public final Path tranfile, dateparm, xreffile, trantype, trancatg, tranrept;

        Args(Path tranfile, Path dateparm, Path xreffile, Path trantype, Path trancatg, Path tranrept) {
            this.tranfile = tranfile;
            this.dateparm = dateparm;
            this.xreffile = xreffile;
            this.trantype = trantype;
            this.trancatg = trancatg;
            this.tranrept = tranrept;
        }

        public static Args parse(String[] argv) {
            Path tranfile = null, dateparm = null, xreffile = null, trantype = null, trancatg = null, tranrept = null;

            for (int i = 0; i < argv.length; i++) {
                switch (argv[i]) {
                    case "--tranfile":
                        tranfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--dateparm":
                        dateparm = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--xreffile":
                        xreffile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--trantype":
                        trantype = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--trancatg":
                        trancatg = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--tranrept":
                        tranrept = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                }
            }

            if (tranfile == null) throw new IllegalArgumentException("--tranfile required");
            if (dateparm == null) throw new IllegalArgumentException("--dateparm required");
            if (xreffile == null) throw new IllegalArgumentException("--xreffile required");
            if (trantype == null) throw new IllegalArgumentException("--trantype required");
            if (trancatg == null) throw new IllegalArgumentException("--trancatg required");
            if (tranrept == null) throw new IllegalArgumentException("--tranrept required");

            CbtrUtils.validateReadable(tranfile, "--tranfile");
            CbtrUtils.validateReadable(dateparm, "--dateparm");
            CbtrUtils.validateReadable(xreffile, "--xreffile");
            CbtrUtils.validateReadable(trantype, "--trantype");
            CbtrUtils.validateReadable(trancatg, "--trancatg");

            return new Args(tranfile, dateparm, xreffile, trantype, trancatg, tranrept);
        }
    }

    public CBTRN03C() {}
}
