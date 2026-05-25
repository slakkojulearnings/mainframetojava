package com.carddemo.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.batch.CBACT02C;
import com.carddemo.codec.Encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Cbact02cVerificationTest {

    @TempDir
    Path tempDir;

    private Path cardFile;

    @BeforeEach
    void setUp() throws IOException {
        cardFile = tempDir.resolve("cardfile.bin");
    }

    @Test
    void readsAndDisplaysCards() throws IOException {
        byte[] cardData = new byte[150];
        String cardNum = "1111111111111111";
        for (int i = 0; i < 16; i++) cardData[i] = (byte) cardNum.charAt(i);

        Files.write(cardFile, cardData);

        CBACT02C.Args args = CBACT02C.harnessArgs(cardFile, Encoding.ASCII, 150);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int rc = new CBACT02C().run(args, baos);

        assertThat(rc).isEqualTo(0);
        byte[] output = baos.toByteArray();
        assertThat(output.length).isGreaterThan(0);
    }

    @Test
    void multipleCards() throws IOException {
        byte[] data = new byte[300];
        String card1 = "1111111111111111";
        String card2 = "2222222222222222";
        for (int i = 0; i < 16; i++) {
            data[i] = (byte) card1.charAt(i);
            data[150 + i] = (byte) card2.charAt(i);
        }

        Files.write(cardFile, data);

        CBACT02C.Args args = CBACT02C.harnessArgs(cardFile, Encoding.ASCII, 150);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int rc = new CBACT02C().run(args, baos);

        assertThat(rc).isEqualTo(0);
    }
}
