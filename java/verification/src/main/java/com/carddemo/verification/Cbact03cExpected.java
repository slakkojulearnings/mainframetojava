package com.carddemo.verification;

import com.carddemo.codec.Encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Analytically-derived expected stdout for {@code CBACT03C} given an input
 * fixture.
 *
 * <p>This implements Option C from {@code verification/README.md}:
 * derived-from-copybook expected output. Valid only for pass-through
 * programs (no arithmetic, no control breaks, no date math).
 *
 * <p>The construction follows the COBOL source byte-for-byte:
 * <ol>
 *   <li>Emit {@code START OF EXECUTION OF PROGRAM CBACT03C\n}.
 *   <li>For each record read in declared length:
 *     <ol>
 *       <li>Emit the record bytes + {@code \n} (the inner DISPLAY from
 *           paragraph {@code 1000-XREFFILE-GET-NEXT}).
 *       <li>Emit the same bytes + {@code \n} (the outer DISPLAY in the main
 *           PERFORM-UNTIL loop body).
 *     </ol>
 *   <li>Emit {@code END OF EXECUTION OF PROGRAM CBACT03C\n}.
 * </ol>
 *
 * <p>Records are read from the fixture file using the same record-framing
 * the Java program uses: ASCII fixtures are CRLF-framed with trailing-space
 * padding to canonical length; EBCDIC fixtures are fixed-length blocks.
 *
 * <p>This generator is the verification reference, not a Java-port artifact.
 * It exists solely so a test can prove the Java port produces what the
 * COBOL source declares it should produce — without needing GnuCOBOL or a
 * mainframe golden.
 */
public final class Cbact03cExpected {

    private static final byte LF = 0x0A;

    private Cbact03cExpected() {}

    /**
     * Generate expected bytes for a given input file.
     *
     * @param xreffile     path to the cardxref fixture
     * @param encoding     character encoding (ASCII or EBCDIC)
     * @param recordLength canonical record length (50 for CARD-XREF-RECORD)
     */
    public static byte[] generate(Path xreffile, Encoding encoding, int recordLength) throws IOException {
        Charset cs = Charset.forName(encoding.charsetName());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writeLine(baos, "START OF EXECUTION OF PROGRAM CBACT03C".getBytes(cs));
        for (byte[] record : readRecords(xreffile, encoding, recordLength)) {
            writeLine(baos, record);
            writeLine(baos, record);
        }
        writeLine(baos, "END OF EXECUTION OF PROGRAM CBACT03C".getBytes(cs));

        return baos.toByteArray();
    }

    /** Read fixture records using the same framing rules as the Java port. */
    private static List<byte[]> readRecords(Path file, Encoding encoding, int recordLength) throws IOException {
        if (encoding == Encoding.ASCII) {
            return readAsciiCrlfRecords(file, recordLength);
        }
        return readEbcdicFixedRecords(file, recordLength);
    }

    private static List<byte[]> readAsciiCrlfRecords(Path file, int recordLength) throws IOException {
        byte[] raw = Files.readAllBytes(file);
        java.util.ArrayList<byte[]> records = new java.util.ArrayList<>();
        int i = 0;
        while (i < raw.length) {
            int start = i;
            // Scan to CRLF, LF, or EOF.
            while (i < raw.length && raw[i] != '\r' && raw[i] != '\n') i++;
            int len = i - start;
            if (len > recordLength) {
                throw new IOException("record at offset " + start + " is " + len
                        + " bytes, exceeds declared length " + recordLength);
            }
            byte[] padded = new byte[recordLength];
            System.arraycopy(raw, start, padded, 0, len);
            // Pad with ASCII space (0x20) — same as the Java port.
            Arrays.fill(padded, len, recordLength, (byte) 0x20);
            records.add(padded);
            // Skip terminator (CRLF, CR, or LF).
            if (i < raw.length && raw[i] == '\r') {
                i++;
                if (i < raw.length && raw[i] == '\n') i++;
            } else if (i < raw.length && raw[i] == '\n') {
                i++;
            }
        }
        return records;
    }

    private static List<byte[]> readEbcdicFixedRecords(Path file, int recordLength) throws IOException {
        byte[] raw = Files.readAllBytes(file);
        if (raw.length % recordLength != 0) {
            throw new IOException("EBCDIC file size " + raw.length
                    + " is not a multiple of record length " + recordLength);
        }
        java.util.ArrayList<byte[]> records = new java.util.ArrayList<>();
        for (int i = 0; i < raw.length; i += recordLength) {
            byte[] rec = new byte[recordLength];
            System.arraycopy(raw, i, rec, 0, recordLength);
            records.add(rec);
        }
        return records;
    }

    private static void writeLine(ByteArrayOutputStream out, byte[] line) {
        out.write(line, 0, line.length);
        out.write(LF);
    }
}
