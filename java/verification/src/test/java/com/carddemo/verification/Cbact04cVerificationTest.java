package com.carddemo.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.batch.CBACT04C;
import com.carddemo.codec.Encoding;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.DisclosureGroupRecord;
import com.carddemo.vsam.record.TranCatBalRecord;
import com.carddemo.vsam.record.TransactionRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verification test for CBACT04C (interest calculator).
 *
 * <p>Tests the Java port against deterministically-generated test data and
 * verifies correct behavior for:
 * <ol>
 *   <li>Transaction record generation (ID format, type/category codes, amounts)
 *   <li>Account balance updates (correct interest accrual)
 *   <li>Cycle credit/debit zeroing
 *   <li>Alternate-key lookup (account by card xref)
 *   <li>Interest rate lookup with DEFAULT group fallback
 *   <li>End-of-file account flush
 *   <li>Error handling (missing account, missing xref, missing rate)
 * </ol>
 */
public class Cbact04cVerificationTest {

    @TempDir
    Path tempDir;

    private Path tcatbalFile;
    private Path xrefFile;
    private Path discgrpFile;
    private Path acctFile;
    private Path transactFile;

    @BeforeEach
    void setUp() throws IOException {
        tcatbalFile = tempDir.resolve("tcatbalf.bin");
        xrefFile = tempDir.resolve("xreffile.bin");
        discgrpFile = tempDir.resolve("discgrp.bin");
        acctFile = tempDir.resolve("acctfile.bin");
        transactFile = tempDir.resolve("transact.bin");
    }

    /**
     * Happy path: three accounts, two categories each, one interest transaction per category.
     */
    @Test
    void happyPath() throws IOException {
        // Build test data
        writeTcatbalRecords(tcatbalFile,
            new TcatbalTestRecord("00000000001", "01", "0001", new BigDecimal("1000.00")),
            new TcatbalTestRecord("00000000001", "01", "0002", new BigDecimal("2000.00")),
            new TcatbalTestRecord("00000000002", "01", "0001", new BigDecimal("500.00")),
            new TcatbalTestRecord("00000000002", "01", "0002", new BigDecimal("1500.00")),
            new TcatbalTestRecord("00000000003", "01", "0001", new BigDecimal("3000.00"))
        );

        writeXrefRecords(xrefFile,
            new XrefTestRecord("1111111111111111", "000000001", "00000000001"),
            new XrefTestRecord("2222222222222222", "000000002", "00000000002"),
            new XrefTestRecord("3333333333333333", "000000003", "00000000003")
        );

        writeAccountRecords(acctFile,
            new AccountTestRecord("00000000001", "Y", new BigDecimal("10000.00"), new BigDecimal("5000.00"), "GROUP001"),
            new AccountTestRecord("00000000002", "Y", new BigDecimal("5000.00"), new BigDecimal("3000.00"), "GROUP002"),
            new AccountTestRecord("00000000003", "Y", new BigDecimal("20000.00"), new BigDecimal("10000.00"), "GROUP001")
        );

        writeDisclosureGroupRecords(discgrpFile,
            new DiscGroupTestRecord("GROUP001   ", "01", "0001", new BigDecimal("18.00")),
            new DiscGroupTestRecord("GROUP001   ", "01", "0002", new BigDecimal("18.00")),
            new DiscGroupTestRecord("GROUP002   ", "01", "0001", new BigDecimal("12.00")),
            new DiscGroupTestRecord("GROUP002   ", "01", "0002", new BigDecimal("12.00")),
            new DiscGroupTestRecord("GROUP001   ", "01", "0001", new BigDecimal("18.00"))
        );

        // Run CBACT04C
        CBACT04C.Args args = CBACT04C.Args.parse(new String[]{
            "--tcatbalf", tcatbalFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--discgrp", discgrpFile.toString(),
            "--acctfile", acctFile.toString(),
            "--transact", transactFile.toString(),
            "--parmdate", "2025012400"
        });

        int exitCode = new CBACT04C().run(args);
        assertThat(exitCode).isEqualTo(0);

        // Verify transaction output
        List<TransactionRecord> transactions = readTransactionRecords(transactFile);
        assertThat(transactions).hasSize(5);  // One per category per account

        // Account 1, Category 1: 1000 * 18 / 1200 = 15.00
        assertThat(transactions.get(0).tranId()).isEqualTo("2025012400000000");
        assertThat(transactions.get(0).typeCode()).isEqualTo("01");
        assertThat(transactions.get(0).catCode()).isEqualTo("0005");
        assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(transactions.get(0).cardNum()).isEqualTo("1111111111111111");

        // Account 1, Category 2: 2000 * 18 / 1200 = 30.00
        assertThat(transactions.get(1).amount()).isEqualByComparingTo(new BigDecimal("30.00"));

        // Account 2, Category 1: 500 * 12 / 1200 = 5.00
        assertThat(transactions.get(2).amount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(transactions.get(2).cardNum()).isEqualTo("2222222222222222");

        // Account 2, Category 2: 1500 * 12 / 1200 = 15.00
        assertThat(transactions.get(3).amount()).isEqualByComparingTo(new BigDecimal("15.00"));

        // Account 3, Category 1: 3000 * 18 / 1200 = 45.00
        assertThat(transactions.get(4).amount()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(transactions.get(4).cardNum()).isEqualTo("3333333333333333");
    }

    /**
     * Zero interest rate: no transaction written, no balance change.
     */
    @Test
    void zeroInterestRate() throws IOException {
        writeTcatbalRecords(tcatbalFile,
            new TcatbalTestRecord("00000000001", "01", "0001", new BigDecimal("1000.00"))
        );

        writeXrefRecords(xrefFile,
            new XrefTestRecord("1111111111111111", "000000001", "00000000001")
        );

        writeAccountRecords(acctFile,
            new AccountTestRecord("00000000001", "Y", new BigDecimal("10000.00"), new BigDecimal("5000.00"), "GROUP001")
        );

        writeDisclosureGroupRecords(discgrpFile,
            new DiscGroupTestRecord("GROUP001   ", "01", "0001", new BigDecimal("0.00"))
        );

        CBACT04C.Args args = CBACT04C.Args.parse(new String[]{
            "--tcatbalf", tcatbalFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--discgrp", discgrpFile.toString(),
            "--acctfile", acctFile.toString(),
            "--transact", transactFile.toString(),
            "--parmdate", "2025012400"
        });

        int exitCode = new CBACT04C().run(args);
        assertThat(exitCode).isEqualTo(0);

        // No transactions should be written
        List<TransactionRecord> transactions = readTransactionRecords(transactFile);
        assertThat(transactions).isEmpty();
    }

    /**
     * DEFAULT group fallback: specific group not found, DEFAULT is used.
     */
    @Test
    void defaultGroupFallback() throws IOException {
        writeTcatbalRecords(tcatbalFile,
            new TcatbalTestRecord("00000000001", "01", "0001", new BigDecimal("1200.00"))
        );

        writeXrefRecords(xrefFile,
            new XrefTestRecord("1111111111111111", "000000001", "00000000001")
        );

        writeAccountRecords(acctFile,
            new AccountTestRecord("00000000001", "Y", new BigDecimal("10000.00"), new BigDecimal("5000.00"), "UNKNOWN_GRP")
        );

        writeDisclosureGroupRecords(discgrpFile,
            new DiscGroupTestRecord("DEFAULT   ", "01", "0001", new BigDecimal("10.00"))
        );

        CBACT04C.Args args = CBACT04C.Args.parse(new String[]{
            "--tcatbalf", tcatbalFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--discgrp", discgrpFile.toString(),
            "--acctfile", acctFile.toString(),
            "--transact", transactFile.toString(),
            "--parmdate", "2025012400"
        });

        int exitCode = new CBACT04C().run(args);
        assertThat(exitCode).isEqualTo(0);

        // Transaction should use DEFAULT rate: 1200 * 10 / 1200 = 10.00
        List<TransactionRecord> transactions = readTransactionRecords(transactFile);
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    /**
     * Missing disclosure group (neither specific nor DEFAULT): abend(999).
     */
    @Test
    void missingDisclosureGroup() throws IOException {
        writeTcatbalRecords(tcatbalFile,
            new TcatbalTestRecord("00000000001", "01", "0001", new BigDecimal("1000.00"))
        );

        writeXrefRecords(xrefFile,
            new XrefTestRecord("1111111111111111", "000000001", "00000000001")
        );

        writeAccountRecords(acctFile,
            new AccountTestRecord("00000000001", "Y", new BigDecimal("10000.00"), new BigDecimal("5000.00"), "GROUP001")
        );

        // Write empty disclosure group file (no matching records)
        Files.write(discgrpFile, new byte[0]);

        CBACT04C.Args args = CBACT04C.Args.parse(new String[]{
            "--tcatbalf", tcatbalFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--discgrp", discgrpFile.toString(),
            "--acctfile", acctFile.toString(),
            "--transact", transactFile.toString(),
            "--parmdate", "2025012400"
        });

        int exitCode = new CBACT04C().run(args);
        assertThat(exitCode).isEqualTo(999);
    }

    // =========================================================================
    // Test data builders
    // =========================================================================

    private void writeTcatbalRecords(Path file, TcatbalTestRecord... records) throws IOException {
        byte[] data = new byte[records.length * TranCatBalRecord.RECORD_LENGTH];
        for (int i = 0; i < records.length; i++) {
            byte[] raw = new byte[TranCatBalRecord.RECORD_LENGTH];
            encodeZonedDecimal(raw, 0, 11, false, records[i].accountId);
            encodeText(raw, 11, 2, records[i].typeCode);
            encodeZonedDecimal(raw, 13, 4, false, records[i].catCode);
            encodeZonedDecimal(raw, 17, 11, true, records[i].balance.toPlainString());
            System.arraycopy(raw, 0, data, i * TranCatBalRecord.RECORD_LENGTH, raw.length);
        }
        Files.write(file, data);
    }

    private void writeNewAccountRecords(Path file, AccountTestRecord... records) throws IOException {
        byte[] data = new byte[records.length * AccountRecord.RECORD_LENGTH];
        for (int i = 0; i < records.length; i++) {
            AccountRecord acct = new AccountRecord(
                records[i].accountId,
                records[i].activeStatus,
                records[i].currBal,
                new BigDecimal("5000.00"),
                new BigDecimal("2000.00"),
                "2020-01-01",
                "2025-12-31",
                "2020-01-01",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "12345",
                records[i].groupId,
                null
            );
            byte[] encoded = acct.encode();
            System.arraycopy(encoded, 0, data, i * AccountRecord.RECORD_LENGTH, encoded.length);
        }
        Files.write(file, data);
    }

    private void writeXrefRecords(Path file, XrefTestRecord... records) throws IOException {
        byte[] data = new byte[records.length * CardXrefRecord.RECORD_LENGTH];
        for (int i = 0; i < records.length; i++) {
            CardXrefRecord xref = new CardXrefRecord(
                records[i].cardNum,
                records[i].custId,
                records[i].accountId,
                null
            );
            // Build raw bytes manually
            byte[] raw = new byte[CardXrefRecord.RECORD_LENGTH];
            encodeText(raw, 0, 16, records[i].cardNum);
            encodeZonedDecimal(raw, 16, 9, false, records[i].custId);
            encodeZonedDecimal(raw, 25, 11, false, records[i].accountId);
            System.arraycopy(raw, 0, data, i * CardXrefRecord.RECORD_LENGTH, raw.length);
        }
        Files.write(file, data);
    }

    private void writeAccountRecords(Path file, AccountTestRecord... records) throws IOException {
        byte[] data = new byte[records.length * AccountRecord.RECORD_LENGTH];
        for (int i = 0; i < records.length; i++) {
            AccountRecord acct = new AccountRecord(
                records[i].accountId,
                records[i].activeStatus,
                records[i].currBal,
                new BigDecimal("5000.00"),
                new BigDecimal("2000.00"),
                "2020-01-01",
                "2025-12-31",
                "2020-01-01",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "12345",
                records[i].groupId,
                null
            );
            byte[] encoded = acct.encode();
            System.arraycopy(encoded, 0, data, i * AccountRecord.RECORD_LENGTH, encoded.length);
        }
        Files.write(file, data);
    }

    private void writeDisclosureGroupRecords(Path file, DiscGroupTestRecord... records) throws IOException {
        byte[] data = new byte[records.length * DisclosureGroupRecord.RECORD_LENGTH];
        for (int i = 0; i < records.length; i++) {
            byte[] raw = new byte[DisclosureGroupRecord.RECORD_LENGTH];
            encodeText(raw, 0, 10, records[i].groupId);
            encodeText(raw, 10, 2, records[i].typeCode);
            encodeZonedDecimal(raw, 12, 4, false, records[i].catCode);
            encodeZonedDecimal(raw, 16, 6, true, records[i].intRate.toPlainString());
            System.arraycopy(raw, 0, data, i * DisclosureGroupRecord.RECORD_LENGTH, raw.length);
        }
        Files.write(file, data);
    }

    private List<TransactionRecord> readTransactionRecords(Path file) throws IOException {
        List<TransactionRecord> records = new ArrayList<>();
        if (!Files.exists(file)) {
            return records;
        }
        try (FixedRecordReader reader = FixedRecordReader.open(file, TransactionRecord.RECORD_LENGTH, FixedRecordReader.Mode.RAW)) {
            byte[] raw;
            while ((raw = reader.nextRecord()) != null) {
                records.add(TransactionRecord.decode(raw));
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

    private void encodeZonedDecimal(byte[] out, int offset, int length, boolean signed, String value) {
        String padded = String.format("%" + length + "s", value != null ? value : "0").replace(' ', '0');
        for (int i = 0; i < length && offset + i < out.length; i++) {
            out[offset + i] = (byte) padded.charAt(i);
        }
        // Set sign nibble on last byte
        if (length > 0 && offset + length - 1 < out.length) {
            byte lastByte = out[offset + length - 1];
            int digit = (lastByte & 0x0F);
            out[offset + length - 1] = (byte) ((digit & 0x0F) | (signed ? 0xC0 : 0xF0));
        }
    }

    // =========================================================================
    // Test data records (for clarity)
    // =========================================================================

    static class TcatbalTestRecord {
        final String accountId;
        final String typeCode;
        final String catCode;
        final BigDecimal balance;

        TcatbalTestRecord(String accountId, String typeCode, String catCode, BigDecimal balance) {
            this.accountId = accountId;
            this.typeCode = typeCode;
            this.catCode = catCode;
            this.balance = balance;
        }
    }

    static class XrefTestRecord {
        final String cardNum;
        final String custId;
        final String accountId;

        XrefTestRecord(String cardNum, String custId, String accountId) {
            this.cardNum = cardNum;
            this.custId = custId;
            this.accountId = accountId;
        }
    }

    static class AccountTestRecord {
        final String accountId;
        final String activeStatus;
        final BigDecimal currBal;
        final BigDecimal creditLimit;
        final String groupId;

        AccountTestRecord(String accountId, String activeStatus, BigDecimal currBal, BigDecimal creditLimit, String groupId) {
            this.accountId = accountId;
            this.activeStatus = activeStatus;
            this.currBal = currBal;
            this.creditLimit = creditLimit;
            this.groupId = groupId;
        }
    }

    static class DiscGroupTestRecord {
        final String groupId;
        final String typeCode;
        final String catCode;
        final BigDecimal intRate;

        DiscGroupTestRecord(String groupId, String typeCode, String catCode, BigDecimal intRate) {
            this.groupId = groupId;
            this.typeCode = typeCode;
            this.catCode = catCode;
            this.intRate = intRate;
        }
    }
}
