package com.carddemo.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.batch.CBSTM03A;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.CustomerRecord;
import com.carddemo.vsam.record.TransactionRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Cbstm03aVerificationTest {

    @TempDir
    Path tempDir;

    private Path trnxFile, xrefFile, custFile, acctFile, stmtFile, htmlFile;

    @BeforeEach
    void setUp() throws IOException {
        trnxFile = tempDir.resolve("trnxfile.bin");
        xrefFile = tempDir.resolve("xref.bin");
        custFile = tempDir.resolve("custfile.bin");
        acctFile = tempDir.resolve("acctfile.bin");
        stmtFile = tempDir.resolve("stmtfile.bin");
        htmlFile = tempDir.resolve("htmlfile.bin");
    }

    @Test
    void plainTextStatement() throws IOException {
        writeTransactions(trnxFile,
            new TransactionRecord("2025012400000001", "01", "0001", "SOURCE", "Groceries",
                new BigDecimal("100.00"), "000000001", "MERCHANT", "CITY", "12345",
                "1111111111111111", "2025-01-24-12.00.00.000000", "2025-01-24-12.00.00.000000")
        );
        writeXrefRecords(xrefFile,
            new CardXrefRecord("1111111111111111", "000000001", "00000000001")
        );
        writeCustomerRecords(custFile,
            new CustomerRecord("000000001", "John", "M", "Doe", "123 Main", "Apt 1", "Seattle",
                "WA", "USA", "98101", "206-555-0001", "206-555-0002", "123456789",
                "GOVID001", "1990-01-15", "EFT001", "Y", "750")
        );
        writeAccountRecords(acctFile,
            new AccountRecord("00000000001", "Y", new BigDecimal("5000.00"), new BigDecimal("10000.00"),
                new BigDecimal("2000.00"), "2020-01-01", "2025-12-31", "2020-01-01",
                BigDecimal.ZERO, BigDecimal.ZERO, "98101", "GROUP001", null)
        );

        CBSTM03A.Args args = CBSTM03A.Args.parse(new String[]{
            "--trnxfile", trnxFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--custfile", custFile.toString(),
            "--acctfile", acctFile.toString(),
            "--stmtfile", stmtFile.toString(),
            "--htmlfile", htmlFile.toString()
        });

        int rc = new CBSTM03A().run(args);
        assertThat(rc).isEqualTo(0);
        assertThat(Files.size(stmtFile)).isGreaterThan(0);
    }

    @Test
    void htmlStatement() throws IOException {
        writeTransactions(trnxFile,
            new TransactionRecord("2025012400000001", "01", "0001", "SOURCE", "Groceries",
                new BigDecimal("100.00"), "000000001", "MERCHANT", "CITY", "12345",
                "1111111111111111", "2025-01-24-12.00.00.000000", "2025-01-24-12.00.00.000000")
        );
        writeXrefRecords(xrefFile,
            new CardXrefRecord("1111111111111111", "000000001", "00000000001")
        );
        writeCustomerRecords(custFile,
            new CustomerRecord("000000001", "Jane", "A", "Smith", "456 Oak", "Suite 2", "Bellevue",
                "WA", "USA", "98004", "425-555-0001", "425-555-0002", "987654321",
                "GOVID002", "1985-06-20", "EFT002", "N", "800")
        );
        writeAccountRecords(acctFile,
            new AccountRecord("00000000001", "Y", new BigDecimal("8000.00"), new BigDecimal("15000.00"),
                new BigDecimal("3000.00"), "2019-01-01", "2026-12-31", "2019-01-01",
                BigDecimal.ZERO, BigDecimal.ZERO, "98004", "GROUP002", null)
        );

        CBSTM03A.Args args = CBSTM03A.Args.parse(new String[]{
            "--trnxfile", trnxFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--custfile", custFile.toString(),
            "--acctfile", acctFile.toString(),
            "--stmtfile", stmtFile.toString(),
            "--htmlfile", htmlFile.toString()
        });

        int rc = new CBSTM03A().run(args);
        assertThat(rc).isEqualTo(0);
        assertThat(Files.size(htmlFile)).isGreaterThan(0);
    }

    @Test
    void multipleAccounts() throws IOException {
        writeTransactions(trnxFile,
            new TransactionRecord("2025012400000001", "01", "0001", "SOURCE", "Groceries",
                new BigDecimal("100.00"), "000000001", "MERCHANT", "CITY", "12345",
                "1111111111111111", "2025-01-24-12.00.00.000000", "2025-01-24-12.00.00.000000"),
            new TransactionRecord("2025012400000002", "01", "0001", "SOURCE", "Gas",
                new BigDecimal("50.00"), "000000002", "MERCHANT", "CITY", "12345",
                "2222222222222222", "2025-01-24-13.00.00.000000", "2025-01-24-13.00.00.000000")
        );
        writeXrefRecords(xrefFile,
            new CardXrefRecord("1111111111111111", "000000001", "00000000001"),
            new CardXrefRecord("2222222222222222", "000000002", "00000000002")
        );
        writeCustomerRecords(custFile,
            new CustomerRecord("000000001", "John", "M", "Doe", "123 Main", "Apt 1", "Seattle",
                "WA", "USA", "98101", "206-555-0001", "206-555-0002", "123456789",
                "GOVID001", "1990-01-15", "EFT001", "Y", "750"),
            new CustomerRecord("000000002", "Jane", "A", "Smith", "456 Oak", "Suite 2", "Bellevue",
                "WA", "USA", "98004", "425-555-0001", "425-555-0002", "987654321",
                "GOVID002", "1985-06-20", "EFT002", "N", "800")
        );
        writeAccountRecords(acctFile,
            new AccountRecord("00000000001", "Y", new BigDecimal("5000.00"), new BigDecimal("10000.00"),
                new BigDecimal("2000.00"), "2020-01-01", "2025-12-31", "2020-01-01",
                BigDecimal.ZERO, BigDecimal.ZERO, "98101", "GROUP001", null),
            new AccountRecord("00000000002", "Y", new BigDecimal("8000.00"), new BigDecimal("15000.00"),
                new BigDecimal("3000.00"), "2019-01-01", "2026-12-31", "2019-01-01",
                BigDecimal.ZERO, BigDecimal.ZERO, "98004", "GROUP002", null)
        );

        CBSTM03A.Args args = CBSTM03A.Args.parse(new String[]{
            "--trnxfile", trnxFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--custfile", custFile.toString(),
            "--acctfile", acctFile.toString(),
            "--stmtfile", stmtFile.toString(),
            "--htmlfile", htmlFile.toString()
        });

        int rc = new CBSTM03A().run(args);
        assertThat(rc).isEqualTo(0);
    }

    @Test
    void noMatchingTransactions() throws IOException {
        Files.write(trnxFile, new byte[0]);
        writeXrefRecords(xrefFile,
            new CardXrefRecord("1111111111111111", "000000001", "00000000001")
        );
        writeCustomerRecords(custFile,
            new CustomerRecord("000000001", "John", "M", "Doe", "123 Main", "Apt 1", "Seattle",
                "WA", "USA", "98101", "206-555-0001", "206-555-0002", "123456789",
                "GOVID001", "1990-01-15", "EFT001", "Y", "750")
        );
        writeAccountRecords(acctFile,
            new AccountRecord("00000000001", "Y", new BigDecimal("5000.00"), new BigDecimal("10000.00"),
                new BigDecimal("2000.00"), "2020-01-01", "2025-12-31", "2020-01-01",
                BigDecimal.ZERO, BigDecimal.ZERO, "98101", "GROUP001", null)
        );

        CBSTM03A.Args args = CBSTM03A.Args.parse(new String[]{
            "--trnxfile", trnxFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--custfile", custFile.toString(),
            "--acctfile", acctFile.toString(),
            "--stmtfile", stmtFile.toString(),
            "--htmlfile", htmlFile.toString()
        });

        int rc = new CBSTM03A().run(args);
        assertThat(rc).isEqualTo(0);
    }

    private void writeTransactions(Path file, TransactionRecord... records) throws IOException {
        byte[] data = new byte[records.length * 350];
        for (int i = 0; i < records.length; i++) {
            System.arraycopy(records[i].encode(), 0, data, i * 350, 350);
        }
        Files.write(file, data);
    }

    private void writeXrefRecords(Path file, CardXrefRecord... records) throws IOException {
        byte[] data = new byte[records.length * 50];
        for (int i = 0; i < records.length; i++) {
            System.arraycopy(records[i].encode(), 0, data, i * 50, 50);
        }
        Files.write(file, data);
    }

    private void writeCustomerRecords(Path file, CustomerRecord... records) throws IOException {
        byte[] data = new byte[records.length * 500];
        for (int i = 0; i < records.length; i++) {
            System.arraycopy(records[i].encode(), 0, data, i * 500, 500);
        }
        Files.write(file, data);
    }

    private void writeAccountRecords(Path file, AccountRecord... records) throws IOException {
        byte[] data = new byte[records.length * 300];
        for (int i = 0; i < records.length; i++) {
            System.arraycopy(records[i].encode(), 0, data, i * 300, 300);
        }
        Files.write(file, data);
    }
}
