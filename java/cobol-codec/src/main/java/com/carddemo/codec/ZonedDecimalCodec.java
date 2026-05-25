package com.carddemo.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

/**
 * Zoned-decimal (COBOL DISPLAY) numeric encode/decode.
 *
 * <p>This codec is the heart of the CardDemo COBOL→Java port. Every signed or
 * unsigned numeric on disk in the daily-batch path is zoned-decimal display.
 * Get this right and 100% byte-equivalence becomes tractable. Get it wrong and
 * every downstream Java program writes wrong bytes.
 *
 * <p>Design choices:
 * <ul>
 *   <li>Decoded values are {@link BigDecimal}, never {@code double} or {@code float}.
 *       Financial math demands exact decimal arithmetic.
 *   <li>The codec is stateless: pass the bytes, the PIC clause, and the encoding,
 *       and get a value back (or vice versa).
 *   <li>Round-trip {@code encode(decode(bytes)).equals(bytes)} is the contract.
 *       Verified by tests on every fixture in {@code app/data/}.
 * </ul>
 *
 * <p>What this codec does NOT do:
 * <ul>
 *   <li>COMP-3 (packed decimal): no on-disk fields use it in the core path.
 *   <li>COMP/BINARY: same.
 *   <li>Edited pictures ({@code PIC ZZZZ9.99-} etc): output-formatting only,
 *       never appears in stored records.
 * </ul>
 */
public final class ZonedDecimalCodec {

    private ZonedDecimalCodec() {}

    /**
     * Decode a zoned-decimal field to {@link BigDecimal}.
     *
     * @param bytes    raw bytes from the file; must be exactly {@code pic.digitCount()} long
     * @param offset   start offset within {@code bytes}
     * @param pic      picture clause describing the field
     * @param encoding character encoding of the bytes ({@link Encoding#EBCDIC} or {@link Encoding#ASCII})
     * @return decoded numeric value with scale = {@code pic.fractionalDigits()}
     * @throws IllegalArgumentException if the bytes are not a valid zoned-decimal field
     */
    public static BigDecimal decode(byte[] bytes, int offset, PicClause pic, Encoding encoding) {
        if (pic.kind() != PicClause.Kind.NUMERIC) {
            throw new IllegalArgumentException("decode requires a NUMERIC pic, got " + pic);
        }
        int len = pic.digitCount();
        if (offset + len > bytes.length) {
            throw new IllegalArgumentException("not enough bytes: need " + len
                    + " starting at offset " + offset + ", but bytes.length = " + bytes.length);
        }
        // Convert the field bytes to a character string in the source encoding.
        Charset charset = Charset.forName(encoding.charsetName());
        String field = new String(bytes, offset, len, charset);

        // Validate: all-but-last char must be plain digits.
        char[] chars = field.toCharArray();
        StringBuilder digits = new StringBuilder(len);
        for (int i = 0; i < len - 1; i++) {
            char c = chars[i];
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("non-digit at position " + i
                        + " of field '" + field + "': '" + c + "'");
            }
            digits.append(c);
        }

        Overpunch.Sign sign;
        char last = chars[len - 1];
        if (pic.signed()) {
            Overpunch.Decoded d = Overpunch.decode(last);
            digits.append((char) ('0' + d.digit));
            sign = d.sign;
        } else {
            // Unsigned: last byte must be a plain digit.
            if (last < '0' || last > '9') {
                throw new IllegalArgumentException("unsigned PIC, but last char is not a digit: '" + last + "'");
            }
            digits.append(last);
            sign = Overpunch.Sign.POSITIVE;
        }

        BigInteger unscaled = new BigInteger(digits.toString());
        if (sign == Overpunch.Sign.NEGATIVE && unscaled.signum() != 0) {
            unscaled = unscaled.negate();
        }
        return new BigDecimal(unscaled, pic.fractionalDigits());
    }

    /** Convenience: decode starting at offset 0. */
    public static BigDecimal decode(byte[] bytes, PicClause pic, Encoding encoding) {
        return decode(bytes, 0, pic, encoding);
    }

    /**
     * Decode preserving sign-of-zero.
     *
     * <p>Use this when round-tripping bytes through decode→encode must
     * produce the original bytes for fields that may legitimately hold
     * negative zero ({@code }} overpunch on an all-zero field). {@link #decode}
     * collapses {@code +0} and {@code -0} into {@link BigDecimal#ZERO}; this
     * method preserves the distinction in the returned {@link ZonedValue}.
     */
    public static ZonedValue decodeFull(byte[] bytes, int offset, PicClause pic, Encoding encoding) {
        if (pic.kind() != PicClause.Kind.NUMERIC) {
            throw new IllegalArgumentException("decodeFull requires a NUMERIC pic, got " + pic);
        }
        int len = pic.digitCount();
        Charset charset = Charset.forName(encoding.charsetName());
        String field = new String(bytes, offset, len, charset);
        char last = field.charAt(len - 1);
        boolean negZero = pic.signed() && (last == '}');
        BigDecimal v = decode(bytes, offset, pic, encoding);
        return new ZonedValue(v, negZero);
    }

    /** Convenience: decodeFull starting at offset 0. */
    public static ZonedValue decodeFull(byte[] bytes, PicClause pic, Encoding encoding) {
        return decodeFull(bytes, 0, pic, encoding);
    }

    /**
     * Encode a {@link BigDecimal} into zoned-decimal bytes.
     *
     * <p>The value's scale must equal {@code pic.fractionalDigits()}. The
     * encoder does <em>not</em> silently rescale; if you need rounding, do it
     * with {@link BigDecimal#setScale(int, java.math.RoundingMode)} before
     * calling.
     *
     * <p>The value's unscaled absolute magnitude must fit in
     * {@code pic.digitCount()} digits. Overflow throws.
     *
     * @param value    the numeric value
     * @param pic      picture clause describing the field
     * @param encoding character encoding for the output bytes
     * @return exactly {@code pic.digitCount()} bytes
     */
    public static byte[] encode(BigDecimal value, PicClause pic, Encoding encoding) {
        if (pic.kind() != PicClause.Kind.NUMERIC) {
            throw new IllegalArgumentException("encode requires a NUMERIC pic, got " + pic);
        }
        if (value.scale() != pic.fractionalDigits()) {
            throw new IllegalArgumentException("value scale " + value.scale()
                    + " does not match PIC fractional digits " + pic.fractionalDigits()
                    + ". Call BigDecimal.setScale(...) first if needed.");
        }
        if (!pic.signed() && value.signum() < 0) {
            throw new IllegalArgumentException("negative value " + value + " cannot fit unsigned PIC " + pic);
        }

        BigInteger unscaled = value.unscaledValue().abs();
        String digitStr = unscaled.toString();
        int needed = pic.digitCount();
        if (digitStr.length() > needed) {
            throw new IllegalArgumentException("value " + value + " has " + digitStr.length()
                    + " digits, but PIC " + pic + " only holds " + needed);
        }
        // Left-pad with zeros to declared digit count.
        StringBuilder padded = new StringBuilder(needed);
        for (int i = digitStr.length(); i < needed; i++) padded.append('0');
        padded.append(digitStr);

        if (pic.signed()) {
            char lastDigit = padded.charAt(needed - 1);
            Overpunch.Sign sign = value.signum() < 0 ? Overpunch.Sign.NEGATIVE : Overpunch.Sign.POSITIVE;
            char overpunch = Overpunch.encode(lastDigit - '0', sign);
            padded.setCharAt(needed - 1, overpunch);
        }

        return padded.toString().getBytes(Charset.forName(encoding.charsetName()));
    }

    /**
     * Encode preserving sign-of-zero.
     *
     * <p>The {@link ZonedValue#isNegativeZero()} flag is honoured only for
     * signed PIC clauses. For unsigned PICs the flag is silently ignored
     * (an unsigned PIC cannot store sign anyway).
     */
    public static byte[] encodeFull(ZonedValue zv, PicClause pic, Encoding encoding) {
        BigDecimal v = zv.value();
        // For nonzero values the sign in BigDecimal already drives behaviour.
        if (v.signum() != 0 || !pic.signed() || !zv.isNegativeZero()) {
            return encode(v, pic, encoding);
        }
        // Explicit negative-zero path: build "0...0}" pattern.
        if (v.scale() != pic.fractionalDigits()) {
            throw new IllegalArgumentException("value scale " + v.scale()
                    + " does not match PIC fractional digits " + pic.fractionalDigits());
        }
        int needed = pic.digitCount();
        StringBuilder s = new StringBuilder(needed);
        for (int i = 0; i < needed - 1; i++) s.append('0');
        s.append(Overpunch.encode(0, Overpunch.Sign.NEGATIVE));
        return s.toString().getBytes(Charset.forName(encoding.charsetName()));
    }
}
