package com.carddemo.vsam;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Reads a fixed-record file one record at a time.
 *
 * <p>Two modes:
 * <ul>
 *   <li><b>RAW</b>: read exactly {@code recordLength} bytes per record, no
 *       terminator handling. Matches mainframe RECFM=FB (the EBCDIC fixtures
 *       in {@code app/data/EBCDIC/}).
 *   <li><b>CRLF</b>: read up to a CRLF, then return the line bytes padded
 *       (with ASCII spaces) to {@code recordLength} if shorter. Matches the
 *       ASCII fixtures in {@code app/data/ASCII/}, including the
 *       {@code cardxref.txt} case where the 14-byte trailing FILLER is
 *       stripped (see {@code docs/concepts/01-ebcdic-and-zoned-decimal.md}).
 * </ul>
 *
 * <p>The reader is single-pass and does not seek. It is the building block
 * for sequential-access readers over fixed-record fixtures. Random-access
 * readers are layered on top by loading the file into a {@code Map} keyed
 * on the primary key field.
 *
 * <p>This class is not thread-safe.
 */
public final class FixedRecordReader implements Closeable {

    /** Record-framing mode. */
    public enum Mode {
        /** Fixed-record: read exactly {@code recordLength} bytes. */
        RAW,
        /** CRLF-terminated: read up to CRLF, pad to {@code recordLength} with spaces (0x20). */
        CRLF
    }

    private final InputStream in;
    private final int recordLength;
    private final Mode mode;
    private long recordsRead;

    public FixedRecordReader(InputStream in, int recordLength, Mode mode) {
        if (recordLength <= 0) {
            throw new IllegalArgumentException("recordLength must be > 0");
        }
        // Wrap with a BufferedInputStream for read efficiency. If caller
        // already buffers, this is harmless.
        this.in = (in instanceof BufferedInputStream) ? in : new BufferedInputStream(in);
        this.recordLength = recordLength;
        this.mode = mode;
        this.recordsRead = 0;
    }

    /** Open a file and return a reader over its records. */
    public static FixedRecordReader open(Path file, int recordLength, Mode mode) throws IOException {
        return new FixedRecordReader(Files.newInputStream(file), recordLength, mode);
    }

    /**
     * Read the next record.
     *
     * @return a byte array of exactly {@code recordLength} bytes, or {@code null} at EOF
     */
    public byte[] nextRecord() throws IOException {
        if (mode == Mode.RAW) {
            return readRaw();
        }
        return readCrlf();
    }

    private byte[] readRaw() throws IOException {
        byte[] buf = new byte[recordLength];
        int n = readFully(buf);
        if (n == 0) return null;  // clean EOF
        if (n < recordLength) {
            throw new IOException("partial record: expected " + recordLength + " bytes, got " + n);
        }
        recordsRead++;
        return buf;
    }

    private byte[] readCrlf() throws IOException {
        // Read until CRLF, LF, or EOF. Whichever comes first.
        // We accept either CRLF or bare LF for tolerance; the canonical
        // CardDemo ASCII files use CRLF, but a tool may have written LF.
        java.io.ByteArrayOutputStream line = new java.io.ByteArrayOutputStream(recordLength);
        int b;
        boolean any = false;
        while ((b = in.read()) != -1) {
            any = true;
            if (b == '\n') {
                break;
            }
            if (b == '\r') {
                // Look ahead one byte for LF (consume it if present).
                in.mark(1);
                int next = in.read();
                if (next != '\n' && next != -1) {
                    in.reset();
                }
                break;
            }
            line.write(b);
        }
        if (!any) return null;  // clean EOF
        byte[] raw = line.toByteArray();
        if (raw.length > recordLength) {
            throw new IOException("record at line " + (recordsRead + 1)
                    + " is " + raw.length + " bytes, exceeds declared length " + recordLength);
        }
        byte[] padded = new byte[recordLength];
        System.arraycopy(raw, 0, padded, 0, raw.length);
        // Pad with ASCII space (0x20). Caller is responsible for knowing
        // this fixture is ASCII; EBCDIC fixtures should use RAW mode.
        Arrays.fill(padded, raw.length, recordLength, (byte) 0x20);
        recordsRead++;
        return padded;
    }

    /** Read exactly {@code buf.length} bytes, or fewer at EOF. Returns how many were read. */
    private int readFully(byte[] buf) throws IOException {
        int total = 0;
        while (total < buf.length) {
            int n = in.read(buf, total, buf.length - total);
            if (n < 0) break;
            total += n;
        }
        return total;
    }

    /** Number of records returned so far (does not count nulls). */
    public long recordsRead() {
        return recordsRead;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
