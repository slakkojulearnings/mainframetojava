package com.carddemo.verification;

/**
 * Byte-level diff utility for verification.
 *
 * <p>Produces a human-readable description of the first divergence between
 * two byte sequences, sufficient to debug a failing byte-equivalence test
 * without staring at hex dumps.
 */
public final class ByteDiff {

    private ByteDiff() {}

    /** Result of a byte diff. {@link #identical} is the success case. */
    public static final class Result {
        public final boolean identical;
        public final int firstDifferenceOffset;     // -1 if identical
        public final int expectedLength;
        public final int actualLength;
        public final String detail;

        Result(boolean identical, int firstDifferenceOffset,
               int expectedLength, int actualLength, String detail) {
            this.identical = identical;
            this.firstDifferenceOffset = firstDifferenceOffset;
            this.expectedLength = expectedLength;
            this.actualLength = actualLength;
            this.detail = detail;
        }

        @Override
        public String toString() {
            if (identical) {
                return "identical (" + actualLength + " bytes)";
            }
            return "DIFFER at offset " + firstDifferenceOffset
                    + " (expected " + expectedLength + " bytes, actual "
                    + actualLength + " bytes): " + detail;
        }
    }

    /** Diff two byte arrays. */
    public static Result diff(byte[] expected, byte[] actual) {
        int min = Math.min(expected.length, actual.length);
        for (int i = 0; i < min; i++) {
            if (expected[i] != actual[i]) {
                return new Result(false, i, expected.length, actual.length,
                        contextAround(expected, actual, i));
            }
        }
        if (expected.length != actual.length) {
            int at = min;
            return new Result(false, at, expected.length, actual.length,
                    "lengths differ; common prefix matches "
                            + min + " bytes; "
                            + (expected.length > actual.length ? "expected is longer" : "actual is longer"));
        }
        return new Result(true, -1, expected.length, actual.length, "");
    }

    private static String contextAround(byte[] expected, byte[] actual, int offset) {
        int start = Math.max(0, offset - 8);
        int end   = Math.min(Math.min(expected.length, actual.length), offset + 9);
        StringBuilder sb = new StringBuilder();
        sb.append("expected[").append(start).append("..").append(end).append(") = ");
        appendHex(sb, expected, start, end);
        sb.append("\n     actual[").append(start).append("..").append(end).append(") = ");
        appendHex(sb, actual, start, end);
        sb.append("\n     expected byte at ").append(offset).append(" = 0x");
        sb.append(String.format("%02X", expected[offset] & 0xFF));
        sb.append("; actual = 0x");
        sb.append(String.format("%02X", actual[offset] & 0xFF));
        return sb.toString();
    }

    private static void appendHex(StringBuilder sb, byte[] data, int start, int end) {
        for (int i = start; i < end; i++) {
            sb.append(String.format("%02X", data[i] & 0xFF));
            if (i + 1 < end) sb.append(' ');
        }
    }
}
