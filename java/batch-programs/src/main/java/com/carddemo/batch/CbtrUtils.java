package com.carddemo.batch;

import com.carddemo.codec.ZonedDecimalCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Shared utilities for CBTRN01C and CBTRN02C batch programs.
 */
public final class CbtrUtils {

    private CbtrUtils() {}

    /**
     * Format an account ID as zoned decimal bytes (11 bytes, unsigned).
     */
    public static byte[] formatZonedDecimal(String value, int length, boolean signed) {
        byte[] out = new byte[length];
        ZonedDecimalCodec.encode(out, 0, length, signed, value);
        return out;
    }

    /**
     * Validate that a file is readable.
     */
    public static void validateReadable(Path p, String name) {
        if (!Files.isReadable(p)) {
            throw new IllegalArgumentException(name + " is not readable: " + p);
        }
    }

    /**
     * Require the next argument in argv (throw if missing).
     */
    public static String requireNext(String[] argv, int i) {
        if (i + 1 >= argv.length) {
            throw new IllegalArgumentException("missing value after " + argv[i]);
        }
        return argv[i + 1];
    }
}
