package com.carddemo.codec;

/**
 * IBM display sign-overpunch table.
 *
 * <p>In COBOL DISPLAY-usage signed numerics, the last digit byte is replaced
 * with a single character that encodes both the digit and the sign:
 *
 * <pre>
 *   '{' = +0   '}' = -0
 *   'A' = +1   'J' = -1
 *   'B' = +2   'K' = -2
 *   ...        ...
 *   'I' = +9   'R' = -9
 * </pre>
 *
 * <p>An unsigned digit character ('0'-'9') in the trailing position decodes as
 * positive, preserving the digit value. An unsigned-numeric COBOL field
 * ({@code PIC 9(n)}, no {@code S}) only ever holds raw digits.
 *
 * <p>The encoding is character-based, not byte-based, so the same table applies
 * whether the file is in ASCII or EBCDIC — the encoding-aware codec converts
 * to character form first.
 *
 * <p>Verified empirically from {@code app/data/ASCII/dailytran.txt} row 2:
 * the {@code TRAN-AMT} field {@code 0000009190}} decodes to -919.00, and the
 * transaction description "Return item" confirms negative sign is correct.
 *
 * <p>This class is stateless and side-effect free.
 */
public final class Overpunch {

    private Overpunch() {}

    /** Sign returned by {@link #decode(char)}. */
    public enum Sign {
        POSITIVE(+1),
        NEGATIVE(-1);

        private final int multiplier;

        Sign(int multiplier) {
            this.multiplier = multiplier;
        }

        /** {@code +1} for POSITIVE, {@code -1} for NEGATIVE. */
        public int multiplier() {
            return multiplier;
        }
    }

    /** Decoded view of an overpunch character: a digit (0..9) and a sign. */
    public static final class Decoded {
        public final int digit;
        public final Sign sign;

        Decoded(int digit, Sign sign) {
            this.digit = digit;
            this.sign = sign;
        }
    }

    /**
     * Decode an overpunch character to its digit and sign.
     *
     * @param c the last character of a signed zoned-decimal field
     * @return digit and sign
     * @throws IllegalArgumentException if {@code c} is not a recognised overpunch
     */
    public static Decoded decode(char c) {
        // Plain digit: positive sign, digit value as-is.
        if (c >= '0' && c <= '9') {
            return new Decoded(c - '0', Sign.POSITIVE);
        }
        // Positive overpunch row: { A B C D E F G H I
        if (c == '{') return new Decoded(0, Sign.POSITIVE);
        if (c >= 'A' && c <= 'I') return new Decoded(c - 'A' + 1, Sign.POSITIVE);
        // Negative overpunch row: } J K L M N O P Q R
        if (c == '}') return new Decoded(0, Sign.NEGATIVE);
        if (c >= 'J' && c <= 'R') return new Decoded(c - 'J' + 1, Sign.NEGATIVE);
        throw new IllegalArgumentException("not a valid overpunch character: '" + c + "' (0x"
                + Integer.toHexString(c) + ")");
    }

    /**
     * Encode a digit + sign into an overpunch character.
     *
     * @param digit 0..9
     * @param sign  positive or negative
     * @return the overpunch character ({@code {}, A-I, }, J-R)
     */
    public static char encode(int digit, Sign sign) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("digit out of range 0..9: " + digit);
        }
        if (sign == Sign.POSITIVE) {
            return digit == 0 ? '{' : (char) ('A' + digit - 1);
        }
        return digit == 0 ? '}' : (char) ('J' + digit - 1);
    }
}
