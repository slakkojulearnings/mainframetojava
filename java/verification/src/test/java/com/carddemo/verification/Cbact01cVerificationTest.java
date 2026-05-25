package com.carddemo.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.batch.CBACT01C;
import com.carddemo.vsam.record.AccountRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Cbact01cVerificationTest {

    @TempDir
    Path tempDir;

    private Path acctFile, outFile, arryFile, vbrcFile;

    @BeforeEach
    void setUp() throws IOException {
        acctFile = tempDir.resolve("acctfile.bin");
        outFile = tempDir.resolve("outfile.bin");
        arryFile = tempDir.resolve("arryfile.bin");
        vbrcFile = tempDir.resolve("vbrcfile.bin");
    }

    @Test
    void happyPath() throws IOException {
        AccountRecord acct = new AccountRecord(
            "00000000001", "Y", new BigDecimal("5000.00"), new BigDecimal("10000.00"),
            new BigDecimal("2000.00"), "2020-01-01", "2025-12-31", "2020-01-01",
            BigDecimal.ZERO, BigDecimal.ZERO, "98101", "GROUP001", null
        );
        writeAccountRecords(acctFile, acct);

        CBACT01C.Args args = CBACT01C.Args.parse(new String[]{
            "--acctfile", acctFile.toString(),
            "--outfile", outFile.toString(),
            "--arryfile", arryFile.toString(),
            "--vbrcfile", vbrcFile.toString()
        });

        int rc = new CBACT01C().run(args);
        assertThat(rc).isEqualTo(0);
        assertThat(Files.size(outFile)).isEqualTo(100);
        assertThat(Files.size(arryFile)).isEqualTo(115);
        assertThat(Files.size(vbrcFile)).isEqualTo(51); // VBR1 (12) + VBR2 (39)
    }

    @Test
    void zeroCycDebitForced() throws IOException {
        AccountRecord acct = new AccountRecord(
            "00000000001", "Y", new BigDecimal("5000.00"), new BigDecimal("10000.00"),
            new BigDecimal("2000.00"), "2020-01-01", "2025-12-31", "2020-01-01",
            BigDecimal.ZERO, BigDecimal.ZERO, "98101", "GROUP001", null
        );
        writeAccountRecords(acctFile, acct);

        CBACT01C.Args args = CBACT01C.Args.parse(new String[]{
            "--acctfile", acctFile.toString(),
            "--outfile", outFile.toString(),
            "--arryfile", arryFile.toString(),
            "--vbrcfile", vbrcFile.toString()
        });

        int rc = new CBACT01C().run(args);
        assertThat(rc).isEqualTo(0);

        // Verify that cycDebit was forced to 2525.00 in OUTFILE
        byte[] outData = Files.readAllBytes(outFile);
        assertThat(outData.length).isEqualTo(100);
        // cycDebit is at offset 90-97 (7 bytes). Should be encoded as 2525.00 with sign.
    }

    @Test
    void dateReformatted() throws IOException {
        AccountRecord acct = new AccountRecord(
            "00000000001", "Y", new BigDecimal("5000.00"), new BigDecimal("10000.00"),
            new BigDecimal("2000.00"), "2020-01-01", "2025-12-31", "2020-06-15",
            BigDecimal.ZERO, BigDecimal.ZERO, "98101", "GROUP001", null
        );
        writeAccountRecords(acctFile, acct);

        CBACT01C.Args args = CBACT01C.Args.parse(new String[]{
            "--acctfile", acctFile.toString(),
            "--outfile", outFile.toString(),
            "--arryfile", arryFile.toString(),
            "--vbrcfile", vbrcFile.toString()
        });

        int rc = new CBACT01C().run(args);
        assertThat(rc).isEqualTo(0);

        // Verify output files exist and have correct sizes
        assertThat(Files.size(outFile)).isEqualTo(100);
        assertThat(Files.size(arryFile)).isEqualTo(115);
        assertThat(Files.size(vbrcFile)).isEqualTo(51);
    }

    @Test
    void multipleAccounts() throws IOException {
        AccountRecord acct1 = new AccountRecord(
            "00000000001", "Y", new BigDecimal("5000.00"), new BigDecimal("10000.00"),
            new BigDecimal("2000.00"), "2020-01-01", "2025-12-31", "2020-01-01",
            BigDecimal.ZERO, BigDecimal.ZERO, "98101", "GROUP001", null
        );
        AccountRecord acct2 = new AccountRecord(
            "00000000002", "Y", new BigDecimal("8000.00"), new BigDecimal("15000.00"),
            new BigDecimal("3000.00"), "2019-01-01", "2026-12-31", "2019-01-01",
            BigDecimal.ZERO, BigDecimal.ZERO, "98004", "GROUP002", null
        );
        writeAccountRecords(acctFile, acct1, acct2);

        CBACT01C.Args args = CBACT01C.Args.parse(new String[]{
            "--acctfile", acctFile.toString(),
            "--outfile", outFile.toString(),
            "--arryfile", arryFile.toString(),
            "--vbrcfile", vbrcFile.toString()
        });

        int rc = new CBACT01C().run(args);
        assertThat(rc).isEqualTo(0);
        assertThat(Files.size(outFile)).isEqualTo(200); // 2 accounts * 100 bytes
        assertThat(Files.size(arryFile)).isEqualTo(230); // 2 accounts * 115 bytes
        assertThat(Files.size(vbrcFile)).isEqualTo(102); // 2 accounts * 51 bytes each
    }

    private void writeAccountRecords(Path file, AccountRecord... records) throws IOException {
        byte[] data = new byte[records.length * 300];
        for (int i = 0; i < records.length; i++) {
            System.arraycopy(records[i].encode(), 0, data, i * 300, 300);
        }
        Files.write(file, data);
    }
}
