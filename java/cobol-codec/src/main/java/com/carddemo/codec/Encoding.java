package com.carddemo.codec;

/**
 * Encoding used by an on-disk COBOL record file.
 *
 * <p>The CardDemo source ships sample data in two encodings:
 * <ul>
 *   <li>{@link #EBCDIC} — IBM-1047 EBCDIC, mainframe-native, fixed-record (no terminators).
 *   <li>{@link #ASCII}  — ASCII translation, CRLF-terminated per record.
 * </ul>
 *
 * <p>Sign overpunch ({@code {} for +0, {@code A}-{@code I} for +1..+9, {@code }} for -0,
 * {@code J}-{@code R} for -1..-9) is encoding-neutral: in EBCDIC the byte for '{' is
 * {@code 0xC0}, in ASCII it is {@code 0x7B}, and both carry the same logical
 * "sign + digit" meaning.
 *
 * <p>This enum exists so the codec can be told which encoding to use without
 * the caller having to know IBM-1047 byte values.
 */
public enum Encoding {

    /** IBM-1047 EBCDIC. Fixed-record on disk, no CRLF. */
    EBCDIC("IBM01047"),

    /** ASCII (US-ASCII / ISO-8859-1 compatible for the byte range CardDemo uses). */
    ASCII("US-ASCII");

    private final String charsetName;

    Encoding(String charsetName) {
        this.charsetName = charsetName;
    }

    /** JVM charset name for this encoding (works with {@link java.nio.charset.Charset#forName(String)}). */
    public String charsetName() {
        return charsetName;
    }
}
