package com.carddemo.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.batch.CBTRN01C;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.DailyTransactionRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verification test suite for CBTRN01C (transaction validation diagnostic).
 */
public class Cbtrn01cVerificationTest {

    @TempDir
    Path tempDir;

    private Path dalytranFile, custFile, xrefFile, cardFile, acctFile, tranFile;

    @BeforeEach
    void setUp() throws IOException {
        dalytranFile = tempDir.resolve("dalytran.bin");
        custFile = tempDir.resolve("custfile.bin");
        xrefFile = tempDir.resolve("xref.bin");
        cardFile = tempDir.resolve("cardfile.bin");
        acctFile = tempDir.resolve("acctfile.bin");
        tranFile = tempDir.resolve("tranfile.bin");
    }

    @Test
    void validCard() throws IOException {
        // Setup: one transaction with valid card and account lookup
        writeDailyTransactions(dalytranFile,
            new TranTestData("2025012400000001", "01", "0001", new BigDecimal("1000.00"), "1111111111111111")
        );
        writeXrefRecords(xrefFile,
            new XrefTestData("1111111111111111", "00000000001")
        );
        writeAccountRecords(acctFile,
            new AcctTestData("00000000001", "Y", new BigDecimal("10000.00"), new BigDecimal("5000.00"))
        );
        // Create empty files for custfile, cardfile, tranfile (opened but not read)
        Files.write(custFile, new byte[0]);
        Files.write(cardFile, new byte[0]);
        Files.write(tranFile, new byte[0]);

        CBTRN01C.Args args = CBTRN01C.Args.parse(new String[]{
            "--dalytran", dalytranFile.toString(),
            "--custfile", custFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--cardfile", cardFile.toString(),
            "--acctfile", acctFile.toString(),
            "--tranfile", tranFile.toString()
        });

        // Capture stdout
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(stdout));

        try {
            int rc = new CBTRN01C().run(args);
            System.setOut(oldOut);
            String output = stdout.toString();

            assertThat(rc).isEqualTo(0);
            assertThat(output).contains("START OF EXECUTION OF PROGRAM CBTRN01C");
            assertThat(output).contains("TRAN: 2025012400000001 CARD: 1111111111111111");
            assertThat(output).contains("XREF FOUND: acctId=00000000001");
            assertThat(output).contains("ACCOUNT FOUND: currBal=10000.00 status=Y");
            assertThat(output).contains("END OF EXECUTION OF PROGRAM CBTRN01C");
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    void invalidCard() throws IOException {
        // Setup: transaction with card not in XREF
        writeDailyTransactions(dalytranFile,
            new TranTestData("2025012400000001", "01", "0001", new BigDecimal("1000.00"), "9999999999999999")
        );
        // Empty xref file
        Files.write(xrefFile, new byte[0]);
        // Create empty files for the others
        Files.write(custFile, new byte[0]);
        Files.write(cardFile, new byte[0]);
        Files.write(acctFile, new byte[0]);
        Files.write(tranFile, new byte[0]);

        CBTRN01C.Args args = CBTRN01C.Args.parse(new String[]{
            "--dalytran", dalytranFile.toString(),
            "--custfile", custFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--cardfile", cardFile.toString(),
            "--acctfile", acctFile.toString(),
            "--tranfile", tranFile.toString()
        });

        // Capture stdout
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(stdout));

        try {
            int rc = new CBTRN01C().run(args);
            System.setOut(oldOut);
            String output = stdout.toString();

            assertThat(rc).isEqualTo(0);
            assertThat(output).contains("TRAN: 2025012400000001 CARD: 9999999999999999");
            assertThat(output).contains("XREF NOT FOUND for card=9999999999999999");
        } finally {
            System.setOut(oldOut);
        }
    }

    // =========================================================================
    // Test data writers
    // =========================================================================

    private void writeDailyTransactions(Path file, TranTestData... records) throws IOException {
        byte[] data = new byte[records.length * 350];
        for (int i = 0; i < records.length; i++) {
            byte[] raw = new byte[350];
            encodeText(raw, 0, 16, records[i].tranId);
            encodeText(raw, 16, 2, records[i].typeCode);
            encodeZoned(raw, 18, 4, false, records[i].catCode);
            encodeZoned(raw, 132, 11, true, records[i].amount.toPlainString());
            encodeText(raw, 262, 16, records[i].cardNum);
            encodeText(raw, 278, 26, records[i].origTs);
            System.arraycopy(raw, 0, data, i * 350, 350);
        }
        Files.write(file, data);
    }

    private void writeXrefRecords(Path file, XrefTestData... records) throws IOException {
        byte[] data = new byte[records.length * 50];
        for (int i = 0; i < records.length; i++) {
            byte[] raw = new byte[50];
            encodeText(raw, 0, 16, records[i].cardNum);
            encodeZoned(raw, 25, 11, false, records[i].acctId);
            System.arraycopy(raw, 0, data, i * 50, 50);
        }
        Files.write(file, data);
    }

    private void writeAccountRecords(Path file, AcctTestData... records) throws IOException {
        byte[] data = new byte[records.length * 300];
        for (int i = 0; i < records.length; i++) {
            AccountRecord acct = new AccountRecord(
                records[i].acctId, records[i].status, records[i].currBal,
                records[i].creditLimit, new BigDecimal("2000.00"),
                "2020-01-01", "2025-12-31", "2020-01-01",
                BigDecimal.ZERO, BigDecimal.ZERO,
                "12345", "GROUP001", null
            );
            System.arraycopy(acct.encode(), 0, data, i * 300, 300);
        }
        Files.write(file, data);
    }

    private void encodeText(byte[] out, int offset, int length, String value) {
        String padded = String.format("%-" + length + "s", value != null ? value : "");
        for (int i = 0; i < length && offset + i < out.length; i++) {
            out[offset + i] = (byte) padded.charAt(i);
        }
    }

    private void encodeZoned(byte[] out, int offset, int length, boolean signed, String value) {
        String padded = String.format("%" + length + "s", value != null ? value : "0").replace(' ', '0');
        for (int i = 0; i < length && offset + i < out.length; i++) {
            out[offset + i] = (byte) padded.charAt(i);
        }
        if (length > 0 && offset + length - 1 < out.length) {
            byte lastByte = out[offset + length - 1];
            int digit = (lastByte & 0x0F);
            out[offset + length - 1] = (byte) ((digit & 0x0F) | (signed ? 0xC0 : 0xF0));
        }
    }

    // Test data records
    static class TranTestData {
        final String tranId, typeCode, catCode, cardNum, origTs;
        final BigDecimal amount;

        TranTestData(String tranId, String typeCode, String catCode, BigDecimal amount, String cardNum) {
            this(tranId, typeCode, catCode, amount, cardNum, "2025-01-24-12.00.00.000000");
        }

        TranTestData(String tranId, String typeCode, String catCode, BigDecimal amount, String cardNum, String origTs) {
            this.tranId = tranId;
            this.typeCode = typeCode;
            this.catCode = catCode;
            this.amount = amount;
            this.cardNum = cardNum;
            this.origTs = origTs;
        }
    }

    static class XrefTestData {
        final String cardNum, acctId;

        XrefTestData(String cardNum, String acctId) {
            this.cardNum = cardNum;
            this.acctId = acctId;
        }
    }

    static class AcctTestData {
        final String acctId, status;
        final BigDecimal currBal, creditLimit;

        AcctTestData(String acctId, String status, BigDecimal currBal, BigDecimal creditLimit) {
            this.acctId = acctId;
            this.status = status;
            this.currBal = currBal;
            this.creditLimit = creditLimit;
        }
    }
}
