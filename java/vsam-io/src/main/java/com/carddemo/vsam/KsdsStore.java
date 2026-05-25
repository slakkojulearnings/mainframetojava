package com.carddemo.vsam;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Write-capable KSDS store for in-memory update + batch flush.
 *
 * <p>Loads all records from a KSDS file into memory, indexed by primary key bytes,
 * supports random read and write (insert/replace), and flushes all records back to
 * a file on close. Used by CBTRN02C for ACCTFILE and TCATBAL-FILE I-O operations.
 *
 * <p>Suitable for demo datasets; for production, use a real database.
 */
public class KsdsStore implements Closeable {

    private final int recordLength;
    private final int keyLength;
    private final Map<ByteArrayKey, byte[]> store;
    private boolean closed;

    private KsdsStore(int recordLength, int keyLength, Map<ByteArrayKey, byte[]> store) {
        this.recordLength = recordLength;
        this.keyLength = keyLength;
        this.store = store;
        this.closed = false;
    }

    /**
     * Load and open a KSDS file for read-write access.
     *
     * @param file the path to the KSDS flat-file dump
     * @param recordLength the fixed record length
     * @param keyLength how many bytes at offset 0 constitute the primary key
     * @param mode the read mode (RAW for binary, CRLF for ASCII/text)
     */
    public static KsdsStore open(Path file, int recordLength, int keyLength, FixedRecordReader.Mode mode) throws IOException {
        Map<ByteArrayKey, byte[]> store = new LinkedHashMap<>();

        try (FixedRecordReader reader = FixedRecordReader.open(file, recordLength, mode)) {
            byte[] record;
            while ((record = reader.nextRecord()) != null) {
                byte[] keyBytes = Arrays.copyOf(record, keyLength);
                store.put(new ByteArrayKey(keyBytes), record);
            }
        }

        return new KsdsStore(recordLength, keyLength, store);
    }

    /**
     * Create an empty KSDS store (no initial file to load).
     */
    public static KsdsStore createEmpty(int recordLength, int keyLength) {
        return new KsdsStore(recordLength, keyLength, new LinkedHashMap<>());
    }

    /**
     * Read by primary key (first N bytes of the record).
     *
     * @param keyBytes the key bytes (must be exactly keyLength bytes)
     */
    public Optional<byte[]> readByKey(byte[] keyBytes) {
        if (closed) {
            throw new IllegalStateException("Store is closed");
        }
        if (keyBytes.length != keyLength) {
            throw new IllegalArgumentException("Key length mismatch: expected " + keyLength + ", got " + keyBytes.length);
        }
        ByteArrayKey key = new ByteArrayKey(keyBytes);
        return Optional.ofNullable(store.get(key));
    }

    /**
     * Write (insert or replace) a record. The key is extracted as record[0..keyLength-1].
     *
     * @param record the full record bytes (must be exactly recordLength bytes)
     */
    public void write(byte[] record) {
        if (closed) {
            throw new IllegalStateException("Store is closed");
        }
        if (record.length != recordLength) {
            throw new IllegalArgumentException("Record length mismatch: expected " + recordLength + ", got " + record.length);
        }
        byte[] keyBytes = Arrays.copyOf(record, keyLength);
        ByteArrayKey key = new ByteArrayKey(keyBytes);
        store.put(key, record);
    }

    /**
     * Return the number of records in the store.
     */
    public int recordCount() {
        return store.size();
    }

    /**
     * Flush all records back to a file (in insertion order).
     *
     * @param outputFile the path to write the updated file to
     */
    public void flush(Path outputFile) throws IOException {
        if (closed) {
            throw new IllegalStateException("Store is closed");
        }
        try (FixedRecordWriter writer = FixedRecordWriter.open(outputFile)) {
            for (byte[] record : store.values()) {
                writer.write(record);
            }
            writer.flush();
        }
    }

    @Override
    public void close() {
        closed = true;
        store.clear();
    }

    /**
     * Wrapper for byte[] to use as a map key (hashCode + equals).
     * Copied from KsdsReader for internal use.
     */
    private static final class ByteArrayKey {
        private final byte[] bytes;
        private final int hash;

        ByteArrayKey(byte[] bytes) {
            this.bytes = bytes;
            this.hash = Arrays.hashCode(bytes);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ByteArrayKey)) return false;
            return Arrays.equals(this.bytes, ((ByteArrayKey) obj).bytes);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
