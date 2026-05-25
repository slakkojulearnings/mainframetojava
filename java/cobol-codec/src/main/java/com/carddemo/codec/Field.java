package com.carddemo.codec;

import java.util.Objects;

/**
 * A named field within a fixed-layout COBOL record.
 *
 * <p>Fields are positioned by offset (zero-based) within the record. The
 * record-layout encoder/decoder uses {@link Field} instances to walk each
 * record in declaration order.
 *
 * <p>For example, the {@code ACCOUNT-RECORD} layout (from
 * {@code app/cpy/CVACT01Y.cpy}, RECLN=300) consists of these fields in order:
 * <ol>
 *   <li>{@code ACCT-ID}             at offset 0,   PIC 9(11),     11 bytes
 *   <li>{@code ACCT-ACTIVE-STATUS}  at offset 11,  PIC X(1),       1 byte
 *   <li>{@code ACCT-CURR-BAL}       at offset 12,  PIC S9(10)V99, 12 bytes
 *   <li>...
 *   <li>{@code FILLER}              at offset 122, PIC X(178),   178 bytes
 * </ol>
 */
public final class Field {

    private final String name;
    private final int offset;
    private final PicClause pic;

    /**
     * Construct a field.
     *
     * @param name   field name as declared in the copybook (e.g. {@code "ACCT-ID"})
     * @param offset zero-based byte offset within the record
     * @param pic    picture clause
     */
    public Field(String name, int offset, PicClause pic) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-blank");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        Objects.requireNonNull(pic, "pic");
        this.name = name;
        this.offset = offset;
        this.pic = pic;
    }

    public String name()   { return name; }
    public int offset()    { return offset; }
    public PicClause pic() { return pic; }

    /** Byte offset just past the end of this field. */
    public int endOffset() {
        return offset + pic.byteLength();
    }

    @Override
    public String toString() {
        return name + " @ " + offset + " " + pic;
    }
}
