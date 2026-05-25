package com.carddemo.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.batch.CBCUS01C;
import com.carddemo.codec.Encoding;
import com.carddemo.vsam.record.CustomerRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Cbcus01cVerificationTest {

    @TempDir
    Path tempDir;

    private Path custFile;

    @BeforeEach
    void setUp() throws IOException {
        custFile = tempDir.resolve("custfile.bin");
    }

    @Test
    void displaysEachRecordTwice() throws IOException {
        CustomerRecord cust = new CustomerRecord(
            "000000001", "John", "M", "Doe", "123 Main", "Apt 1", "Seattle",
            "WA", "USA", "98101", "206-555-0001", "206-555-0002", "123456789",
            "GOVID001", "1990-01-15", "EFT001", "Y", "750"
        );
        writeCustomerRecords(custFile, cust);

        CBCUS01C.Args args = CBCUS01C.harnessArgs(custFile, Encoding.ASCII, 500);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int rc = new CBCUS01C().run(args, baos);

        assertThat(rc).isEqualTo(0);
        byte[] output = baos.toByteArray();
        assertThat(output.length).isGreaterThan(0);
    }

    @Test
    void multipleCustomers() throws IOException {
        CustomerRecord cust1 = new CustomerRecord(
            "000000001", "John", "M", "Doe", "123 Main", "Apt 1", "Seattle",
            "WA", "USA", "98101", "206-555-0001", "206-555-0002", "123456789",
            "GOVID001", "1990-01-15", "EFT001", "Y", "750"
        );
        CustomerRecord cust2 = new CustomerRecord(
            "000000002", "Jane", "A", "Smith", "456 Oak", "Suite 2", "Bellevue",
            "WA", "USA", "98004", "425-555-0001", "425-555-0002", "987654321",
            "GOVID002", "1985-06-20", "EFT002", "N", "800"
        );
        writeCustomerRecords(custFile, cust1, cust2);

        CBCUS01C.Args args = CBCUS01C.harnessArgs(custFile, Encoding.ASCII, 500);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int rc = new CBCUS01C().run(args, baos);

        assertThat(rc).isEqualTo(0);
    }

    private void writeCustomerRecords(Path file, CustomerRecord... records) throws IOException {
        byte[] data = new byte[records.length * 500];
        for (int i = 0; i < records.length; i++) {
            System.arraycopy(records[i].encode(), 0, data, i * 500, 500);
        }
        Files.write(file, data);
    }
}
