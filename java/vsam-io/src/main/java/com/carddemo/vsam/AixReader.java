package com.carddemo.vsam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Alternate Index (AIX) reader for KSDS files.
 *
 * <p>Provides lookup by an alternate key (e.g., account ID in a card cross-reference file).
 * Wraps a KsdsReader and searches by partial key within loaded records.
 */
public class AixReader {

    private final KsdsReader reader;
    private final int keyOffset;
    private final int keyLength;

    /**
     * Wrap a KsdsReader to provide alternate-key access.
     *
     * @param reader the underlying KSDS reader
     * @param keyOffset byte offset within the record where the alternate key starts
     * @param keyLength length of the alternate key in bytes
     */
    public AixReader(KsdsReader reader, int keyOffset, int keyLength) {
        this.reader = reader;
        this.keyOffset = keyOffset;
        this.keyLength = keyLength;
    }

    /**
     * Look up a record by alternate key (first match).
     *
     * @param altKeyBytes the alternate key bytes to search for
     * @return the first record matching the alternate key, or empty if not found
     */
    public Optional<byte[]> readByAlternateKey(byte[] altKeyBytes) {
        if (altKeyBytes.length != keyLength) {
            throw new IllegalArgumentException("Key length mismatch: expected " + keyLength + ", got " + altKeyBytes.length);
        }
        for (byte[] record : reader.allRecords()) {
            if (arrayEquals(record, keyOffset, keyLength, altKeyBytes)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    /**
     * Find all records matching the alternate key.
     *
     * @param altKeyBytes the alternate key bytes to search for
     */
    public List<byte[]> readAllByAlternateKey(byte[] altKeyBytes) {
        if (altKeyBytes.length != keyLength) {
            throw new IllegalArgumentException("Key length mismatch: expected " + keyLength + ", got " + altKeyBytes.length);
        }
        List<byte[]> results = new ArrayList<>();
        for (byte[] record : reader.allRecords()) {
            if (arrayEquals(record, keyOffset, keyLength, altKeyBytes)) {
                results.add(record);
            }
        }
        return results;
    }

    /**
     * Direct access to the underlying reader for other operations.
     */
    public KsdsReader reader() {
        return reader;
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
}
