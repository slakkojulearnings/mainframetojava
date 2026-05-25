package com.carddemo.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.batch.CBTRN03C;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.TranCatRecord;
import com.carddemo.vsam.record.TranTypeRecord;
import com.carddemo.vsam.record.TransactionRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Cbtrn03cVerificationTest {

    @TempDir
    Path tempDir;

    private Path tranFile, dateParamFile, xrefFile, tranTypeFile, tranCatgFile, reportFile;

    @BeforeEach
    void setUp() throws IOException {
        tranFile = tempDir.resolve("tranfile.bin");
        dateParamFile = tempDir.resolve("dateparam.bin");
        xrefFile = tempDir.resolve("xref.bin");
        tranTypeFile = tempDir.resolve("trantype.bin");
        tranCatgFile = tempDir.resolve("trancatg.bin");
        reportFile = tempDir.resolve("report.bin");
    }

    @Test
    void happyPath() throws IOException {
        writeTransactions(tranFile,
            new TransactionData("2025012400000001", "01", "0001", new BigDecimal("1000.00"),
                "1111111111111111", "2025-01-24-12.00.00.000000", "Test Merchant")
        );
        writeDateParam(dateParamFile, "2025-01-24", "2025-01-25");
        writeXrefRecords(xrefFile,
            new XrefData("1111111111111111", "00000000001")
        );
        writeTranTypeRecords(tranTypeFile,
            new TranTypeData("01", "Purchase")
        );
        writeTranCatRecords(tranCatgFile,
            new TranCatData("01", "0001", "Groceries")
        );

        CBTRN03C.Args args = CBTRN03C.Args.parse(new String[]{
            "--tranfile", tranFile.toString(),
            "--dateparm", dateParamFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--trantype", tranTypeFile.toString(),
            "--trancatg", tranCatgFile.toString(),
            "--tranrept", reportFile.toString()
        });

        int rc = new CBTRN03C().run(args);
        assertThat(rc).isEqualTo(0);
        assertThat(Files.size(reportFile)).isGreaterThan(0);
    }

    @Test
    void dateRangeFilter() throws IOException {
        writeTransactions(tranFile,
            new TransactionData("2025012400000001", "01", "0001", new BigDecimal("1000.00"),
                "1111111111111111", "2025-01-24-12.00.00.000000", "Test"),
            new TransactionData("2025012600000002", "01", "0001", new BigDecimal("500.00"),
                "2222222222222222", "2025-01-26-12.00.00.000000", "Test")
        );
        writeDateParam(dateParamFile, "2025-01-24", "2025-01-25");
        writeXrefRecords(xrefFile,
            new XrefData("1111111111111111", "00000000001"),
            new XrefData("2222222222222222", "00000000002")
        );
        writeTranTypeRecords(tranTypeFile, new TranTypeData("01", "Purchase"));
        writeTranCatRecords(tranCatgFile, new TranCatData("01", "0001", "Groceries"));

        CBTRN03C.Args args = CBTRN03C.Args.parse(new String[]{
            "--tranfile", tranFile.toString(),
            "--dateparm", dateParamFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--trantype", tranTypeFile.toString(),
            "--trancatg", tranCatgFile.toString(),
            "--tranrept", reportFile.toString()
        });

        int rc = new CBTRN03C().run(args);
        assertThat(rc).isEqualTo(0);
    }

    @Test
    void accountBreak() throws IOException {
        writeTransactions(tranFile,
            new TransactionData("2025012400000001", "01", "0001", new BigDecimal("1000.00"),
                "1111111111111111", "2025-01-24-12.00.00.000000", "Tran1"),
            new TransactionData("2025012400000002", "01", "0001", new BigDecimal("500.00"),
                "2222222222222222", "2025-01-24-13.00.00.000000", "Tran2")
        );
        writeDateParam(dateParamFile, "2025-01-24", "2025-01-24");
        writeXrefRecords(xrefFile,
            new XrefData("1111111111111111", "00000000001"),
            new XrefData("2222222222222222", "00000000002")
        );
        writeTranTypeRecords(tranTypeFile, new TranTypeData("01", "Purchase"));
        writeTranCatRecords(tranCatgFile, new TranCatData("01", "0001", "Groceries"));

        CBTRN03C.Args args = CBTRN03C.Args.parse(new String[]{
            "--tranfile", tranFile.toString(),
            "--dateparm", dateParamFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--trantype", tranTypeFile.toString(),
            "--trancatg", tranCatgFile.toString(),
            "--tranrept", reportFile.toString()
        });

        int rc = new CBTRN03C().run(args);
        assertThat(rc).isEqualTo(0);
    }

    @Test
    void pageBreak() throws IOException {
        List<TransactionData> transactions = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            transactions.add(new TransactionData(
                String.format("2025012400%05d", i), "01", "0001", new BigDecimal("100.00"),
                "1111111111111111", "2025-01-24-12.00.00.000000", "Tran" + i
            ));
        }
        writeTransactions(tranFile, transactions.toArray(new TransactionData[0]));
        writeDateParam(dateParamFile, "2025-01-24", "2025-01-24");
        writeXrefRecords(xrefFile, new XrefData("1111111111111111", "00000000001"));
        writeTranTypeRecords(tranTypeFile, new TranTypeData("01", "Purchase"));
        writeTranCatRecords(tranCatgFile, new TranCatData("01", "0001", "Groceries"));

        CBTRN03C.Args args = CBTRN03C.Args.parse(new String[]{
            "--tranfile", tranFile.toString(),
            "--dateparm", dateParamFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--trantype", tranTypeFile.toString(),
            "--trancatg", tranCatgFile.toString(),
            "--tranrept", reportFile.toString()
        });

        int rc = new CBTRN03C().run(args);
        assertThat(rc).isEqualTo(0);
    }

    @Test
    void missingXref() throws IOException {
        writeTransactions(tranFile,
            new TransactionData("2025012400000001", "01", "0001", new BigDecimal("1000.00"),
                "9999999999999999", "2025-01-24-12.00.00.000000", "Test")
        );
        writeDateParam(dateParamFile, "2025-01-24", "2025-01-25");
        Files.write(xrefFile, new byte[0]);
        writeTranTypeRecords(tranTypeFile, new TranTypeData("01", "Purchase"));
        writeTranCatRecords(tranCatgFile, new TranCatData("01", "0001", "Groceries"));

        CBTRN03C.Args args = CBTRN03C.Args.parse(new String[]{
            "--tranfile", tranFile.toString(),
            "--dateparam", dateParamFile.toString(),
            "--xreffile", xrefFile.toString(),
            "--trantype", tranTypeFile.toString(),
            "--trancatg", tranCatgFile.toString(),
            "--tranrept", reportFile.toString()
        });

        int rc = new CBTRN03C().run(args);
        assertThat(rc).isEqualTo(999);
    }

    private void writeTransactions(Path file, TransactionData... records) throws IOException {
        byte[] data = new byte[records.length * 350];
        for (int i = 0; i < records.length; i++) {
            TransactionRecord tran = new TransactionRecord(
                records[i].tranId, records[i].typeCode, records[i].catCode, "SOURCE",
                records[i].desc, records[i].amount, "000000001", "MERCHANT", "CITY", "12345",
                records[i].cardNum, records[i].origTs, records[i].origTs
            );
            System.arraycopy(tran.encode(), 0, data, i * 350, 350);
        }
        Files.write(file, data);
    }

    private void writeDateParam(Path file, String startDate, String endDate) throws IOException {
        byte[] data = new byte[80];
        String startPadded = String.format("%-10s", startDate);
        String endPadded = String.format("%-10s", endDate);
        for (int i = 0; i < 10; i++) data[i] = (byte) startPadded.charAt(i);
        data[10] = (byte) ' ';
        for (int i = 0; i < 10; i++) data[11 + i] = (byte) endPadded.charAt(i);
        Files.write(file, data);
    }

    private void writeXrefRecords(Path file, XrefData... records) throws IOException {
        byte[] data = new byte[records.length * 50];
        for (int i = 0; i < records.length; i++) {
            CardXrefRecord xref = new CardXrefRecord(
                records[i].cardNum, records[i].custId, records[i].acctId
            );
            System.arraycopy(xref.encode(), 0, data, i * 50, 50);
        }
        Files.write(file, data);
    }

    private void writeTranTypeRecords(Path file, TranTypeData... records) throws IOException {
        byte[] data = new byte[records.length * 60];
        for (int i = 0; i < records.length; i++) {
            TranTypeRecord rec = new TranTypeRecord(records[i].typeCode, records[i].typeDesc);
            byte[] encoded = new byte[60];
            String typeCode = String.format("%-2s", records[i].typeCode);
            String typeDesc = String.format("%-50s", records[i].typeDesc);
            for (int j = 0; j < 2; j++) encoded[j] = (byte) typeCode.charAt(j);
            for (int j = 0; j < 50; j++) encoded[2 + j] = (byte) typeDesc.charAt(j);
            System.arraycopy(encoded, 0, data, i * 60, 60);
        }
        Files.write(file, data);
    }

    private void writeTranCatRecords(Path file, TranCatData... records) throws IOException {
        byte[] data = new byte[records.length * 60];
        for (int i = 0; i < records.length; i++) {
            byte[] encoded = new byte[60];
            String typeCode = String.format("%-2s", records[i].typeCode);
            String catCode = String.format("%04d", Integer.parseInt(records[i].catCode));
            String catDesc = String.format("%-50s", records[i].catDesc);
            for (int j = 0; j < 2; j++) encoded[j] = (byte) typeCode.charAt(j);
            for (int j = 0; j < 4; j++) encoded[2 + j] = (byte) catCode.charAt(j);
            for (int j = 0; j < 50; j++) encoded[6 + j] = (byte) catDesc.charAt(j);
            System.arraycopy(encoded, 0, data, i * 60, 60);
        }
        Files.write(file, data);
    }

    static class TransactionData {
        String tranId, typeCode, catCode, cardNum, origTs, desc;
        BigDecimal amount;

        TransactionData(String tranId, String typeCode, String catCode, BigDecimal amount,
                       String cardNum, String origTs, String desc) {
            this.tranId = tranId;
            this.typeCode = typeCode;
            this.catCode = catCode;
            this.amount = amount;
            this.cardNum = cardNum;
            this.origTs = origTs;
            this.desc = desc;
        }
    }

    static class XrefData {
        String cardNum, custId, acctId;

        XrefData(String cardNum, String custId, String acctId) {
            this.cardNum = cardNum;
            this.custId = custId;
            this.acctId = acctId;
        }

        XrefData(String cardNum, String acctId) {
            this(cardNum, "000000001", acctId);
        }
    }

    static class TranTypeData {
        String typeCode, typeDesc;

        TranTypeData(String typeCode, String typeDesc) {
            this.typeCode = typeCode;
            this.typeDesc = typeDesc;
        }
    }

    static class TranCatData {
        String typeCode, catCode, catDesc;

        TranCatData(String typeCode, String catCode, String catDesc) {
            this.typeCode = typeCode;
            this.catCode = catCode;
            this.catDesc = catDesc;
        }
    }
}
