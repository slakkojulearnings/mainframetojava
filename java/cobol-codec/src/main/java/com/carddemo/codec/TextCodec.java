package com.carddemo.codec;

import java.nio.charset.Charset;

/**
 * Encode/decode for {@code PIC X(n)} fixed-width text fields.
 *
 * <p>COBOL text fields are right-padded with spaces to their declared length.
 * In EBCDIC the space byte is {@code 0x40}; in ASCII it is {@code 0x20}. This
 * codec handles both.
 *
 * <p>On decode, trailing spaces are <em>preserved</em> — the caller decides
 * whether to trim. This matters because some fields use trailing spaces as
 * data (e.g. fixed-width identifiers) and trimming would lose information.
 */
public final class TextCodec {

    private TextCodec() {}

    /**
     * Decode a fixed-width text field.
     *
     * @param bytes    raw bytes
     * @param offset   start offset
     * @param pic      picture clause; must be {@link PicClause.Kind#TEXT}
     * @param encoding character encoding
     * @return decoded string of exactly {@code pic.characterLength()} characters,
     *         including any trailing spaces
     */
    public static String decode(byte[] bytes, int offset, PicClause pic, Encoding encoding) {
        if (pic.kind() != PicClause.Kind.TEXT) {
            throw new IllegalArgumentException("decode requires a TEXT pic, got " + pic);
        }
        int len = pic.characterLength();
        if (offset + len > bytes.length) {
            throw new IllegalArgumentException("not enough bytes: need " + len
                    + " starting at offset " + offset + ", but bytes.length = " + bytes.length);
        }
        return new String(bytes, offset, len, Charset.forName(encoding.charsetName()));
    }

    /** Convenience: decode starting at offset 0. */
    public static String decode(byte[] bytes, PicClause pic, Encoding encoding) {
        return decode(bytes, 0, pic, encoding);
    }

    /**
     * Encode a string to a fixed-width text field.
     *
     * <p>If the string is shorter than the declared length, the output is
     * right-padded with space characters (encoding-aware). If longer, the
     * encoder throws — silent truncation would discard data.
     *
     * @param value    the string value
     * @param pic      picture clause; must be {@link PicClause.Kind#TEXT}
     * @param encoding character encoding for the output bytes
     * @return exactly {@code pic.characterLength()} bytes
     */
    public static byte[] encode(String value, PicClause pic, Encoding encoding) {
        if (pic.kind() != PicClause.Kind.TEXT) {
            throw new IllegalArgumentException("encode requires a TEXT pic, got " + pic);
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        int len = pic.characterLength();
        if (value.length() > len) {
            throw new IllegalArgumentException("string '" + value + "' length " + value.length()
                    + " exceeds PIC X(" + len + ")");
        }
        // Pad the character string to the target length, then encode.
        StringBuilder padded = new StringBuilder(len);
        padded.append(value);
        while (padded.length() < len) padded.append(' ');
        return padded.toString().getBytes(Charset.forName(encoding.charsetName()));
    }
}
