package com.carddemo.vsam;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fixed-length record writer for sequential output files (RECFM=F, LRECL=N).
 *
 * <p>Writes raw fixed-length records with no line terminators or separators.
 * Each call to write() appends exactly N bytes to the file.
 */
public class FixedRecordWriter implements Closeable {

    private final OutputStream out;
    private final int recordLength;
    private boolean closed;

    private FixedRecordWriter(OutputStream out, int recordLength) {
        this.out = out;
        this.recordLength = recordLength;
        this.closed = false;
    }

    /**
     * Open a file for writing fixed-length records.
     *
     * @param file the output file path
     * @param recordLength the fixed record length (inferred from first write if not specified)
     */
    public static FixedRecordWriter open(Path file, int recordLength) throws IOException {
        OutputStream out = Files.newOutputStream(file);
        return new FixedRecordWriter(out, recordLength);
    }

    /**
     * Open a file for writing, with record length inferred from first write.
     */
    public static FixedRecordWriter open(Path file) throws IOException {
        OutputStream out = Files.newOutputStream(file);
        return new FixedRecordWriter(out, -1);  // Length to be determined on first write
    }

    /**
     * Write a fixed-length record.
     *
     * @param record the record bytes (must match recordLength)
     */
    public void write(byte[] record) throws IOException {
        if (closed) {
            throw new IllegalStateException("Writer is closed");
        }
        if (recordLength > 0 && record.length != recordLength) {
            throw new IllegalArgumentException("Record length mismatch: expected " + recordLength + ", got " + record.length);
        }
        out.write(record);
    }

    /**
     * Write multiple records.
     */
    public void writeAll(Iterable<byte[]> records) throws IOException {
        for (byte[] record : records) {
            write(record);
        }
    }

    /**
     * Flush the output stream.
     */
    public void flush() throws IOException {
        if (!closed) {
            out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            out.close();
            closed = true;
        }
    }
}
