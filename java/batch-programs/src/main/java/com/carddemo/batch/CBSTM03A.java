package com.carddemo.batch;

import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.CustomerRecord;
import com.carddemo.vsam.record.TransactionRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CBSTM03A {

    public static void main(String[] args) {
        Args parsed;
        try {
            parsed = Args.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(999);
            return;
        }
        int exitCode = new CBSTM03A().run(parsed);
        System.exit(exitCode);
    }

    public int run(Args args) {
        try {
            System.out.println("START OF EXECUTION OF PROGRAM CBSTM03A");

            KsdsReader custReader = KsdsReader.open(args.custfile, 500, FixedRecordReader.Mode.RAW);
            KsdsReader acctReader = KsdsReader.open(args.acctfile, 300, FixedRecordReader.Mode.RAW);

            FixedRecordWriter stmtWriter = FixedRecordWriter.open(args.stmtfile);
            FixedRecordWriter htmlWriter = FixedRecordWriter.open(args.htmlfile);

            try (FixedRecordReader trnxReader = FixedRecordReader.open(args.trnxfile, 350, FixedRecordReader.Mode.RAW);
                 FixedRecordReader xrefReader = FixedRecordReader.open(args.xreffile, 50, FixedRecordReader.Mode.RAW)) {

                // Pre-load transactions
                Map<String, List<TransactionRecord>> tranMap = new LinkedHashMap<>();
                byte[] raw;
                while ((raw = trnxReader.nextRecord()) != null) {
                    TransactionRecord tran = TransactionRecord.decode(raw);
                    String cardNum = tran.cardNum().trim();
                    tranMap.computeIfAbsent(cardNum, k -> new ArrayList<>()).add(tran);
                }

                // Generate statements
                int xrefCount = 0;
                while ((raw = xrefReader.nextRecord()) != null) {
                    xrefCount++;
                    CardXrefRecord xref = CardXrefRecord.decode(raw);

                    Optional<byte[]> custRaw = custReader.readByKey(CbtrUtils.formatZonedDecimal(xref.custId(), 9, false));
                    if (!custRaw.isPresent()) {
                        System.err.println("ERROR: Customer not found for custId=" + xref.custId());
                        return 999;
                    }
                    CustomerRecord cust = CustomerRecord.decode(custRaw.get());

                    Optional<byte[]> acctRaw = acctReader.readByKey(CbtrUtils.formatZonedDecimal(xref.accountId(), 11, false));
                    if (!acctRaw.isPresent()) {
                        System.err.println("ERROR: Account not found for acctId=" + xref.accountId());
                        return 999;
                    }
                    AccountRecord acct = AccountRecord.decode(acctRaw.get());

                    List<TransactionRecord> transactions = tranMap.getOrDefault(xref.cardNum().trim(), new ArrayList<>());
                    BigDecimal totalAmt = transactions.stream()
                        .map(TransactionRecord::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                    writePlainStatement(stmtWriter, cust, acct, xref, transactions, totalAmt);
                    writeHtmlStatement(htmlWriter, cust, acct, xref, transactions, totalAmt);
                }

                System.out.println("STATEMENTS GENERATED: " + xrefCount);
                System.out.println("END OF EXECUTION OF PROGRAM CBSTM03A");

                stmtWriter.close();
                htmlWriter.close();
                custReader.close();
                acctReader.close();

                return 0;
            }

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 999;
        }
    }

    private void writePlainStatement(FixedRecordWriter writer, CustomerRecord cust, AccountRecord acct,
                                     CardXrefRecord xref, List<TransactionRecord> transactions,
                                     BigDecimal totalAmt) throws IOException {
        writeLine(writer, "****************************** START OF STATEMENT ******************************");
        writeLine(writer, cust.firstName() + " " + cust.lastName());
        writeLine(writer, cust.addrLine1());
        writeLine(writer, cust.addrLine2());
        writeLine(writer, cust.addrLine3() + " " + cust.addrStateCode() + " " + cust.addrCountryCode() + " " + cust.addrZip());
        writeLine(writer, "--------------------------------------------------------------------------------");
        writeLine(writer, "                                 Basic Details");
        writeLine(writer, "--------------------------------------------------------------------------------");
        writeLine(writer, "Account ID         : " + acct.accountId());
        writeLine(writer, "Current Balance    : " + formatCurrency(acct.currBal()));
        writeLine(writer, "FICO Score         : " + cust.ficoScore());
        writeLine(writer, "--------------------------------------------------------------------------------");
        writeLine(writer, "                          TRANSACTION SUMMARY");
        writeLine(writer, "--------------------------------------------------------------------------------");
        writeLine(writer, "Tran ID              Tran Details                                      Tran Amount");
        writeLine(writer, "--------------------------------------------------------------------------------");
        for (TransactionRecord tran : transactions) {
            writeLine(writer, String.format("%-16s%-49s$%15.2f", tran.tranId(), tran.desc(), tran.amount()));
        }
        writeLine(writer, "--------------------------------------------------------------------------------");
        writeLine(writer, String.format("%-49s$%15.2f", "Total EXP:", totalAmt));
        writeLine(writer, "****************************** END OF STATEMENT ********************************");
    }

    private void writeLine(FixedRecordWriter writer, String text) throws IOException {
        byte[] line = new byte[80];
        String padded = String.format("%-80s", text != null ? text : "");
        for (int i = 0; i < 80 && i < padded.length(); i++) {
            line[i] = (byte) padded.charAt(i);
        }
        writer.write(line);
    }

    private void writeHtmlStatement(FixedRecordWriter writer, CustomerRecord cust, AccountRecord acct,
                                    CardXrefRecord xref, List<TransactionRecord> transactions,
                                    BigDecimal totalAmt) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html><head><style>\n");
        html.append("body { font-family: Segoe UI, Arial; font-size: 12px; }\n");
        html.append("table { width: 70%; margin: 0 auto; border-collapse: collapse; }\n");
        html.append("th, td { padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #33FF5E; font-weight: bold; }\n");
        html.append("tr:nth-child(even) { background-color: #f2f2f2; }\n");
        html.append("</style></head><body>\n");
        html.append("<table>\n");
        html.append("<tr style=\"background-color:#1d1d96b3;color:white;\">\n");
        html.append("<th colspan=\"3\">Statement for Account Number: ").append(acct.accountId()).append("</th>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#FFAF33;\">\n");
        html.append("<th colspan=\"3\">Bank of XYZ / 410 Terry Ave N / Seattle WA 99999</th>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#f2f2f2;\">\n");
        html.append("<td colspan=\"3\">").append(cust.firstName()).append(" ").append(cust.lastName()).append("</td>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#f2f2f2;\">\n");
        html.append("<td colspan=\"3\">").append(cust.addrLine1()).append("</td>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#33FFD1;text-align:center;\">\n");
        html.append("<th colspan=\"3\">Basic Details</th>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#f2f2f2;\">\n");
        html.append("<td colspan=\"3\"><p>Account ID: ").append(acct.accountId()).append("</p></td>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#f2f2f2;\">\n");
        html.append("<td colspan=\"3\"><p>Current Balance: ").append(formatCurrency(acct.currBal())).append("</p></td>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#f2f2f2;\">\n");
        html.append("<td colspan=\"3\"><p>FICO Score: ").append(cust.ficoScore()).append("</p></td>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#33FFD1;text-align:center;\">\n");
        html.append("<th colspan=\"3\">Transaction Summary</th>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#33FF5E;\">\n");
        html.append("<th style=\"width:25%;\">Tran ID</th>\n");
        html.append("<th style=\"width:55%;\">Tran Details</th>\n");
        html.append("<th style=\"width:20%;text-align:right;\">Amount</th>\n");
        html.append("</tr>\n");
        for (TransactionRecord tran : transactions) {
            html.append("<tr style=\"background-color:#f2f2f2;\">\n");
            html.append("<td>").append(tran.tranId()).append("</td>\n");
            html.append("<td>").append(tran.desc()).append("</td>\n");
            html.append("<td style=\"text-align:right;\">").append(String.format("$%.2f", tran.amount())).append("</td>\n");
            html.append("</tr>\n");
        }
        html.append("<tr style=\"background-color:#f2f2f2;\">\n");
        html.append("<td colspan=\"2\">Total EXP:</td>\n");
        html.append("<td style=\"text-align:right;\">").append(String.format("$%.2f", totalAmt)).append("</td>\n");
        html.append("</tr>\n");
        html.append("<tr style=\"background-color:#f2f2f2;\">\n");
        html.append("<td colspan=\"3\" style=\"text-align:center;\">End of Statement</td>\n");
        html.append("</tr>\n");
        html.append("</table></body></html>\n");

        writeHtmlLines(writer, html.toString());
    }

    private void writeHtmlLines(FixedRecordWriter writer, String html) throws IOException {
        int pos = 0;
        while (pos < html.length()) {
            int end = Math.min(pos + 100, html.length());
            String chunk = html.substring(pos, end);
            byte[] line = new byte[100];
            String padded = String.format("%-100s", chunk);
            for (int i = 0; i < 100 && i < padded.length(); i++) {
                line[i] = (byte) padded.charAt(i);
            }
            writer.write(line);
            pos = end;
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return amount.negate().toPlainString() + "-";
        }
        return amount.toPlainString();
    }

    public static final class Args {
        public final Path trnxfile, xreffile, custfile, acctfile, stmtfile, htmlfile;

        Args(Path trnxfile, Path xreffile, Path custfile, Path acctfile, Path stmtfile, Path htmlfile) {
            this.trnxfile = trnxfile;
            this.xreffile = xreffile;
            this.custfile = custfile;
            this.acctfile = acctfile;
            this.stmtfile = stmtfile;
            this.htmlfile = htmlfile;
        }

        public static Args parse(String[] argv) {
            Path trnxfile = null, xreffile = null, custfile = null, acctfile = null, stmtfile = null, htmlfile = null;

            for (int i = 0; i < argv.length; i++) {
                switch (argv[i]) {
                    case "--trnxfile":
                        trnxfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--xreffile":
                        xreffile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--custfile":
                        custfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--acctfile":
                        acctfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--stmtfile":
                        stmtfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                    case "--htmlfile":
                        htmlfile = Paths.get(CbtrUtils.requireNext(argv, i++));
                        break;
                }
            }

            if (trnxfile == null) throw new IllegalArgumentException("--trnxfile required");
            if (xreffile == null) throw new IllegalArgumentException("--xreffile required");
            if (custfile == null) throw new IllegalArgumentException("--custfile required");
            if (acctfile == null) throw new IllegalArgumentException("--acctfile required");
            if (stmtfile == null) throw new IllegalArgumentException("--stmtfile required");
            if (htmlfile == null) throw new IllegalArgumentException("--htmlfile required");

            CbtrUtils.validateReadable(trnxfile, "--trnxfile");
            CbtrUtils.validateReadable(xreffile, "--xreffile");
            CbtrUtils.validateReadable(custfile, "--custfile");
            CbtrUtils.validateReadable(acctfile, "--acctfile");

            return new Args(trnxfile, xreffile, custfile, acctfile, stmtfile, htmlfile);
        }
    }

    public CBSTM03A() {}
}
