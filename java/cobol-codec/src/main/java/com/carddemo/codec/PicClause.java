package com.carddemo.codec;

import java.util.Objects;

/**
 * Parsed representation of a COBOL DISPLAY-usage PIC clause.
 *
 * <p>This codec deliberately supports only the subset of PIC clauses that the
 * CardDemo daily-batch path actually uses on disk:
 * <ul>
 *   <li>{@code PIC X(n)} — fixed-width text.
 *   <li>{@code PIC 9(n)} — unsigned zoned-decimal integer.
 *   <li>{@code PIC 9(n)V9(m)} — unsigned zoned-decimal with implied decimal.
 *   <li>{@code PIC S9(n)} — signed zoned-decimal integer (overpunch on last digit).
 *   <li>{@code PIC S9(n)V9(m)} — signed zoned-decimal with implied decimal.
 * </ul>
 *
 * <p>Not supported (and not present in core on-disk records): COMP, COMP-3,
 * BINARY, edited pictures, REDEFINES, OCCURS as part of a single field. These
 * are handled at the record-layout level, not here.
 *
 * <p>The byte length on disk is always {@link #digitCount()} for numeric fields
 * (sign occupies the last digit byte, not an extra byte) or
 * {@link #characterLength()} for text fields.
 *
 * <p>Empirical reference: every signed numeric field in {@code app/cpy/CV*Y.cpy}
 * uses display-overpunch with the sign on the last digit. See
 * {@code docs/concepts/01-ebcdic-and-zoned-decimal.md} for worked examples.
 */
public final class PicClause {

    /** Kind of PIC clause. */
    public enum Kind {
        /** {@code PIC X(n)} — fixed-width text, space-padded on the right. */
        TEXT,
        /** Numeric DISPLAY ({@code PIC 9(n)V9(m)} or {@code PIC S9(n)V9(m)}). */
        NUMERIC
    }

    private final Kind kind;
    private final int characterLength;  // for TEXT
    private final int integerDigits;    // digits before V (numeric)
    private final int fractionalDigits; // digits after V (numeric)
    private final boolean signed;       // numeric only

    private PicClause(Kind kind, int characterLength, int integerDigits, int fractionalDigits, boolean signed) {
        this.kind = kind;
        this.characterLength = characterLength;
        this.integerDigits = integerDigits;
        this.fractionalDigits = fractionalDigits;
        this.signed = signed;
    }

    /** {@code PIC X(n)} factory. */
    public static PicClause text(int characterLength) {
        if (characterLength <= 0) {
            throw new IllegalArgumentException("characterLength must be > 0, got " + characterLength);
        }
        return new PicClause(Kind.TEXT, characterLength, 0, 0, false);
    }

    /**
     * Numeric factory.
     *
     * @param integerDigits    digits before the implied decimal (the {@code n} in {@code 9(n)})
     * @param fractionalDigits digits after the implied decimal (the {@code m} in {@code V9(m)}; 0 if no {@code V} clause)
     * @param signed           whether the picture is {@code S9...}
     */
    public static PicClause numeric(int integerDigits, int fractionalDigits, boolean signed) {
        if (integerDigits < 0 || fractionalDigits < 0) {
            throw new IllegalArgumentException("digit counts must be >= 0");
        }
        int total = integerDigits + fractionalDigits;
        if (total <= 0) {
            throw new IllegalArgumentException("must have at least one digit");
        }
        return new PicClause(Kind.NUMERIC, 0, integerDigits, fractionalDigits, signed);
    }

    public Kind kind() { return kind; }

    /** For TEXT: declared character length. */
    public int characterLength() {
        if (kind != Kind.TEXT) {
            throw new IllegalStateException("characterLength only defined for TEXT");
        }
        return characterLength;
    }

    /** For NUMERIC: total number of digits (integer + fractional). One byte per digit on disk. */
    public int digitCount() {
        if (kind != Kind.NUMERIC) {
            throw new IllegalStateException("digitCount only defined for NUMERIC");
        }
        return integerDigits + fractionalDigits;
    }

    public int integerDigits()    { return integerDigits; }
    public int fractionalDigits() { return fractionalDigits; }
    public boolean signed()       { return signed; }

    /** On-disk byte length of a value matching this picture. */
    public int byteLength() {
        return kind == Kind.TEXT ? characterLength : digitCount();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PicClause)) return false;
        PicClause p = (PicClause) o;
        return kind == p.kind
            && characterLength == p.characterLength
            && integerDigits == p.integerDigits
            && fractionalDigits == p.fractionalDigits
            && signed == p.signed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, characterLength, integerDigits, fractionalDigits, signed);
    }

    @Override
    public String toString() {
        if (kind == Kind.TEXT) {
            return "PIC X(" + characterLength + ")";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("PIC ");
        if (signed) sb.append('S');
        sb.append("9(").append(integerDigits).append(')');
        if (fractionalDigits > 0) {
            sb.append("V9(").append(fractionalDigits).append(')');
        }
        return sb.toString();
    }
}
