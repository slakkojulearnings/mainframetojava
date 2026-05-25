package com.carddemo.vsam;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory KSDS (Keyed Sequential Data Set) reader for fixed-length records.
 *
 * <p>Loads all records from a flat file on open, indexes them by primary key bytes,
 * and provides O(1) random lookup plus sequential scan. Suitable for demo datasets;
 * for production, use a real key-value store or database.
 */
public class KsdsReader implements Closeable {

    private final int recordLength;
    private final Map<ByteArrayKey, byte[]> index;
    private final List<byte[]> sequentialOrder;
    private boolean closed;

    private KsdsReader(int recordLength, Map<ByteArrayKey, byte[]> index, List<byte[]> sequentialOrder) {
        this.recordLength = recordLength;
        this.index = index;
        this.sequentialOrder = sequentialOrder;
        this.closed = false;
    }

    /**
     * Open and load a KSDS file.
     *
     * @param file the path to the KSDS flat-file dump
     * @param recordLength the fixed record length
     * @param mode the read mode (RAW for binary, CRLF for ASCII/text)
     */
    public static KsdsReader open(Path file, int recordLength, FixedRecordReader.Mode mode) throws IOException {
        Map<ByteArrayKey, byte[]> index = new LinkedHashMap<>();
        List<byte[]> sequentialOrder = new ArrayList<>();

        try (FixedRecordReader reader = FixedRecordReader.open(file, recordLength, mode)) {
            for (byte[] record = reader.nextRecord(); record != null; record = reader.nextRecord()) {
                ByteArrayKey key = new ByteArrayKey(record);
                index.put(key, record);
                sequentialOrder.add(record);
            }
        }

        return new KsdsReader(recordLength, index, sequentialOrder);
    }

    /**
     * Read by primary key (exact match).
     *
     * @param keyBytes the key to look up (first N bytes of the record)
     */
    public Optional<byte[]> readByKey(byte[] keyBytes) {
        if (closed) {
            throw new IllegalStateException("Reader is closed");
        }
        ByteArrayKey key = new ByteArrayKey(keyBytes);
        return Optional.ofNullable(index.get(key));
    }

    /**
     * Read by partial key (e.g., account ID within a larger composite key).
     *
     * @param recordBytes the full record to extract key from (used for iteration)
     * @param keyOffset offset within the record where the partial key starts
     * @param keyLength length of the partial key
     * @return records matching the partial key
     */
    public List<byte[]> readByPartialKey(byte[] keyBytes, int keyOffset, int keyLength) {
        if (closed) {
            throw new IllegalStateException("Reader is closed");
        }
        List<byte[]> results = new ArrayList<>();
        for (byte[] record : sequentialOrder) {
            if (arrayEquals(record, keyOffset, keyLength, keyBytes)) {
                results.add(record);
            }
        }
        return results;
    }

    /**
     * Sequential scan of all records in load order.
     */
    public Iterator<byte[]> sequentialScan() {
        if (closed) {
            throw new IllegalStateException("Reader is closed");
        }
        return sequentialOrder.iterator();
    }

    /**
     * List all records.
     */
    public List<byte[]> allRecords() {
        if (closed) {
            throw new IllegalStateException("Reader is closed");
        }
        return new ArrayList<>(sequentialOrder);
    }

    /**
     * Number of records loaded.
     */
    public int recordCount() {
        return sequentialOrder.size();
    }

    @Override
    public void close() {
        closed = true;
        index.clear();
        sequentialOrder.clear();
    }

    // Helper: compare a subset of two byte arrays
    private static boolean arrayEquals(byte[] arr, int offset, int length, byte[] expected) {
        if (offset + length > arr.length || expected.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (arr[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Wrapper for byte[] to use as a map key (hashCode + equals).
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
