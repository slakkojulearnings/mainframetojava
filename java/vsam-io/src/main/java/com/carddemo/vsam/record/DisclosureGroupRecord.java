package com.carddemo.vsam.record;

import java.math.BigDecimal;
import java.util.Arrays;
import com.carddemo.codec.Field;
import com.carddemo.codec.PicClause;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.codec.TextCodec;

/**
 * Disclosure Group record from CVTRA02Y.cpy, LRECL=50 bytes.
 *
 * <p>Key: acctGroupId (10) + tranTypeCode (2) + tranCatCode (4) = 16 bytes.
 * Contains interest rates for account groups and transaction categories.
 */
public final class DisclosureGroupRecord {

    public static final int RECORD_LENGTH = 50;

    public static final Field[] LAYOUT = {
        new Field("DIS-ACCT-GROUP-ID",  0, PicClause.text(10)),
        new Field("DIS-TRAN-TYPE-CD",  10, PicClause.text(2)),
        new Field("DIS-TRAN-CAT-CD",   12, PicClause.numeric(4, 0, false)),
        new Field("DIS-INT-RATE",      16, PicClause.numeric(4, 2, true)),
        new Field("FILLER",            22, PicClause.text(28)),
    };

    private final String groupId;       // PIC X(10)
    private final String typeCode;      // PIC X(2)
    private final String catCode;       // PIC 9(4)
    private final BigDecimal intRate;   // PIC S9(4)V99 — annual percentage, e.g. 18.00
    private final byte[] raw;           // original 50 bytes

    public DisclosureGroupRecord(String groupId, String typeCode, String catCode,
                                BigDecimal intRate, byte[] raw) {
        this.groupId = groupId;
        this.typeCode = typeCode;
        this.catCode = catCode;
        this.intRate = intRate;
        this.raw = raw;
    }

    public String groupId()      { return groupId; }
    public String typeCode()     { return typeCode; }
    public String catCode()      { return catCode; }
    public BigDecimal intRate()  { return intRate; }
    public byte[] raw()          { return raw; }

    /** Composite key: groupId + typeCode + catCode (16 bytes). */
    public byte[] primaryKey() {
        byte[] key = new byte[16];
        System.arraycopy(raw, 0, key, 0, 16);
        return key;
    }

    public static DisclosureGroupRecord decode(byte[] raw) {
        if (raw.length != RECORD_LENGTH) {
            throw new IllegalArgumentException("Expected 50 bytes, got " + raw.length);
        }
        byte[] copy = Arrays.copyOf(raw, RECORD_LENGTH);

        String groupId = TextCodec.decode(raw, 0, 10);
        String typeCode = TextCodec.decode(raw, 10, 2);
        String catCode = ZonedDecimalCodec.decode(raw, 12, 4, false).toPlainString();
        BigDecimal intRate = ZonedDecimalCodec.decode(raw, 16, 6, true);

        return new DisclosureGroupRecord(groupId, typeCode, catCode, intRate, copy);
    }

    @Override
    public String toString() {
        return "DisclosureGroupRecord{" +
               "groupId='" + groupId + '\'' +
               ", typeCode='" + typeCode + '\'' +
               ", catCode='" + catCode + '\'' +
               ", intRate=" + intRate +
               '}';
    }
}
