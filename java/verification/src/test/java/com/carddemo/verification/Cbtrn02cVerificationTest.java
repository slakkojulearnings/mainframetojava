package com.carddemo.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.batch.CBTRN02C;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.DailyTransactionRecord;
import com.carddemo.vsam.record.TransactionRecord;
import com.carddemo.vsam.record.TranCatBalRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verification test suite for CBTRN02C (transaction posting).
 */
public class Cbtrn02cVerificationTest {

    @TempDir
    Path tempDir;

    private Path dalytranFile, xrefFile, acctFile, tcatbalFile, transactFile, rejectFile;

    @BeforeEach
    void setUp() throws IOException {
        dalytranFile = tempDir.resolve("dalytran.bin");
        xrefFile = tempDir.resolve("xref.bin");
        acctFile = tempDir.resolve("acctfile.bin");
        tcatbalFile = tempDir.resolve("tcatbalf.bin");
        transactFile = tempDir.resolve("transact.bin");
        rejectFile = tempDir.resolve("reject.bin");
    }

    @Test
    void happyPath() throws IOException {
        // Setup: one valid transaction
        writeDailyTransactions(dalytranFile,
            new TranTestData("2025012400000001", "01", "0001", new BigDecimal("1000.00"), "1111111111111111")
        );
        writeXrefRecords(xrefFile,
            new XrefTestData("1111111111111111", "00000000001")
        );
        writeAccountRecords(acctFile,
            new AcctTestData("00000000001", "Y", new BigDecimal("10000.00"), new BigDecimal("5000.00"), "GROUP001")
        );
        writeTcatbalRecords(tcatbalFile,
            new TcatTestData("00000000001", "01", "0001", new BigDecimal("5000.00"))
        );

        CBTRN02C.Args args = CBTRN02C.Args.parse(new String[]{
            "--dalytran", dalytranFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--acctfile", acctFile.toString(),
            "--tcatbalf", tcatbalFile.toString(),
            "--transact", transactFile.toString(),
            "--dalyrejs", rejectFile.toString()
        });

        int rc = new CBTRN02C().run(args);
        assertThat(rc).isEqualTo(0);

        // Verify transaction written
        List<TransactionRecord> transactions = readTransactions(transactFile);
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).tranId()).isEqualTo("2025012400000001");
        assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("1000.00"));

        // Verify TCATBAL updated
        List<TranCatBalRecord> tcatbals = readTcatbals(tcatbalFile);
        assertThat(tcatbals).hasSize(1);
        assertThat(tcatbals.get(0).balance()).isEqualByComparingTo(new BigDecimal("6000.00"));

        // No rejects
        assertThat(Files.size(rejectFile)).isEqualTo(0);
    }

    @Test
    void invalidCard() throws IOException {
        writeDailyTransactions(dalytranFile,
            new TranTestData("2025012400000001", "01", "0001", new BigDecimal("1000.00"), "9999999999999999")
        );
        // Empty xref — card not found
        Files.write(xrefFile, new byte[0]);
        Files.write(acctFile, new byte[0]);
        Files.write(tcatbalFile, new byte[0]);

        CBTRN02C.Args args = CBTRN02C.Args.parse(new String[]{
            "--dalytran", dalytranFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--acctfile", acctFile.toString(),
            "--tcatbalf", tcatbalFile.toString(),
            "--transact", transactFile.toString(),
            "--dalyrejs", rejectFile.toString()
        });

        int rc = new CBTRN02C().run(args);
        assertThat(rc).isEqualTo(4);  // Rejects occurred

        // No transaction written
        assertThat(Files.size(transactFile)).isEqualTo(0);

        // Reject written with code 100
        assertThat(Files.size(rejectFile)).isEqualTo(430);
        byte[] rejectBytes = Files.readAllBytes(rejectFile);
        String code = new String(rejectBytes, 350, 4);
        assertThat(code).isEqualTo("0100");
    }

    @Test
    void accountNotFound() throws IOException {
        writeDailyTransactions(dalytranFile,
            new TranTestData("2025012400000001", "01", "0001", new BigDecimal("1000.00"), "1111111111111111")
        );
        writeXrefRecords(xrefFile,
            new XrefTestData("1111111111111111", "00000000001")
        );
        // Empty account file
        Files.write(acctFile, new byte[0]);
        Files.write(tcatbalFile, new byte[0]);

        CBTRN02C.Args args = CBTRN02C.Args.parse(new String[]{
            "--dalytran", dalytranFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--acctfile", acctFile.toString(),
            "--tcatbalf", tcatbalFile.toString(),
            "--transact", transactFile.toString(),
            "--dalyrejs", rejectFile.toString()
        });

        int rc = new CBTRN02C().run(args);
        assertThat(rc).isEqualTo(4);

        // Reject with code 101
        byte[] rejectBytes = Files.readAllBytes(rejectFile);
        String code = new String(rejectBytes, 350, 4);
        assertThat(code).isEqualTo("0101");
    }

    @Test
    void overlimitTransaction() throws IOException {
        writeDailyTransactions(dalytranFile,
            new TranTestData("2025012400000001", "01", "0001", new BigDecimal("8000.00"), "1111111111111111")
        );
        writeXrefRecords(xrefFile,
            new XrefTestData("1111111111111111", "00000000001")
        );
        // Credit limit 5000, cycle credit 1000, cycle debit 500 → with 8000 transaction, exceeds limit
        writeAccountRecords(acctFile,
            new AcctTestData("00000000001", "Y", new BigDecimal("10000.00"), new BigDecimal("5000.00"), "GROUP001",
                new BigDecimal("1000.00"), new BigDecimal("500.00"))
        );
        Files.write(tcatbalFile, new byte[0]);

        CBTRN02C.Args args = CBTRN02C.Args.parse(new String[]{
            "--dalytran", dalytranFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--acctfile", acctFile.toString(),
            "--tcatbalf", tcatbalFile.toString(),
            "--transact", transactFile.toString(),
            "--dalyrejs", rejectFile.toString()
        });

        int rc = new CBTRN02C().run(args);
        assertThat(rc).isEqualTo(4);

        // Reject with code 102
        byte[] rejectBytes = Files.readAllBytes(rejectFile);
        String code = new String(rejectBytes, 350, 4);
        assertThat(code).isEqualTo("0102");
    }

    @Test
    void expiredAccount() throws IOException {
        // Transaction dated 2025-01-30, account expired 2025-01-25
        writeDailyTransactions(dalytranFile,
            new TranTestData("2025013000000001", "01", "0001", new BigDecimal("1000.00"), "1111111111111111",
                "2025-01-30-12.00.00.000000")
        );
        writeXrefRecords(xrefFile,
            new XrefTestData("1111111111111111", "00000000001")
        );
        writeAccountRecords(acctFile,
            new AcctTestData("00000000001", "Y", new BigDecimal("10000.00"), new BigDecimal("5000.00"), "GROUP001",
                "2025-01-25")  // Expiration date
        );
        Files.write(tcatbalFile, new byte[0]);

        CBTRN02C.Args args = CBTRN02C.Args.parse(new String[]{
            "--dalytran", dalytranFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--acctfile", acctFile.toString(),
            "--tcatbalf", tcatbalFile.toString(),
            "--transact", transactFile.toString(),
            "--dalyrejs", rejectFile.toString()
        });

        int rc = new CBTRN02C().run(args);
        assertThat(rc).isEqualTo(4);

        // Reject with code 103
        byte[] rejectBytes = Files.readAllBytes(rejectFile);
        String code = new String(rejectBytes, 350, 4);
        assertThat(code).isEqualTo("0103");
    }

    @Test
    void tcatbalCreate() throws IOException {
        writeDailyTransactions(dalytranFile,
            new TranTestData("2025012400000001", "01", "0001", new BigDecimal("1000.00"), "1111111111111111")
        );
        writeXrefRecords(xrefFile,
            new XrefTestData("1111111111111111", "00000000001")
        );
        writeAccountRecords(acctFile,
            new AcctTestData("00000000001", "Y", new BigDecimal("10000.00"), new BigDecimal("5000.00"), "GROUP001")
        );
        // Empty tcatbal — new record will be created
        Files.write(tcatbalFile, new byte[0]);

        CBTRN02C.Args args = CBTRN02C.Args.parse(new String[]{
            "--dalytran", dalytranFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--acctfile", acctFile.toString(),
            "--tcatbalf", tcatbalFile.toString(),
            "--transact", transactFile.toString(),
            "--dalyrejs", rejectFile.toString()
        });

        int rc = new CBTRN02C().run(args);
        assertThat(rc).isEqualTo(0);

        // New TCATBAL record created with balance = 1000.00
        List<TranCatBalRecord> tcatbals = readTcatbals(tcatbalFile);
        assertThat(tcatbals).hasSize(1);
        assertThat(tcatbals.get(0).balance()).isEqualByComparingTo(new BigDecimal("1000.00"));
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
                "2020-01-01", records[i].expDate, "2020-01-01",
                records[i].cycCredit, records[i].cycDebit,
                "12345", "GROUP001", null
            );
            System.arraycopy(acct.encode(), 0, data, i * 300, 300);
        }
        Files.write(file, data);
    }

    private void writeTcatbalRecords(Path file, TcatTestData... records) throws IOException {
        byte[] data = new byte[records.length * 50];
        for (int i = 0; i < records.length; i++) {
            TranCatBalRecord tcb = new TranCatBalRecord(
                records[i].acctId, records[i].typeCode, records[i].catCode,
                records[i].balance, null
            );
            System.arraycopy(tcb.encode(), 0, data, i * 50, 50);
        }
        Files.write(file, data);
    }

    private List<TransactionRecord> readTransactions(Path file) throws IOException {
        List<TransactionRecord> records = new ArrayList<>();
        if (!Files.exists(file) || Files.size(file) == 0) return records;
        try (FixedRecordReader reader = FixedRecordReader.open(file, 350, FixedRecordReader.Mode.RAW)) {
            byte[] raw;
            while ((raw = reader.nextRecord()) != null) {
                records.add(TransactionRecord.decode(raw));
            }
        }
        return records;
    }

    private List<TranCatBalRecord> readTcatbals(Path file) throws IOException {
        List<TranCatBalRecord> records = new ArrayList<>();
        if (!Files.exists(file) || Files.size(file) == 0) return records;
        try (FixedRecordReader reader = FixedRecordReader.open(file, 50, FixedRecordReader.Mode.RAW)) {
            byte[] raw;
            while ((raw = reader.nextRecord()) != null) {
                records.add(TranCatBalRecord.decode(raw));
            }
        }
        return records;
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
        final String acctId, status, expDate;
        final BigDecimal currBal, creditLimit, cycCredit, cycDebit;

        AcctTestData(String acctId, String status, BigDecimal currBal, BigDecimal creditLimit, String groupId) {
            this(acctId, status, currBal, creditLimit, groupId, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        AcctTestData(String acctId, String status, BigDecimal currBal, BigDecimal creditLimit, String groupId,
                    BigDecimal cycCredit, BigDecimal cycDebit) {
            this(acctId, status, currBal, creditLimit, groupId, "2025-12-31", cycCredit, cycDebit);
        }

        AcctTestData(String acctId, String status, BigDecimal currBal, BigDecimal creditLimit, String groupId, String expDate) {
            this(acctId, status, currBal, creditLimit, groupId, expDate, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        AcctTestData(String acctId, String status, BigDecimal currBal, BigDecimal creditLimit, String groupId,
                    String expDate, BigDecimal cycCredit, BigDecimal cycDebit) {
            this.acctId = acctId;
            this.status = status;
            this.currBal = currBal;
            this.creditLimit = creditLimit;
            this.expDate = expDate;
            this.cycCredit = cycCredit;
            this.cycDebit = cycDebit;
        }
    }

    static class TcatTestData {
        final String acctId, typeCode, catCode;
        final BigDecimal balance;

        TcatTestData(String acctId, String typeCode, String catCode, BigDecimal balance) {
            this.acctId = acctId;
            this.typeCode = typeCode;
            this.catCode = catCode;
            this.balance = balance;
        }
    }
}
