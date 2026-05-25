package com.carddemo.vsam.record;

import java.math.BigDecimal;
import java.util.Arrays;
import com.carddemo.codec.Field;
import com.carddemo.codec.PicClause;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.codec.TextCodec;

/**
 * Transaction Category Balance record from CVTRA01Y.cpy, LRECL=50 bytes.
 *
 * <p>Key: accountId (11) + typeCode (2) + catCode (4) = 17 bytes.
 * Used to track spending by category for interest calculation.
 */
public final class TranCatBalRecord {

    public static final int RECORD_LENGTH = 50;

    // Layout: matches CVTRA01Y.cpy
    public static final Field[] LAYOUT = {
        new Field("TRANCAT-ACCT-ID",    0, PicClause.numeric(11, 0, false)),
        new Field("TRANCAT-TYPE-CD",   11, PicClause.text(2)),
        new Field("TRANCAT-CD",        13, PicClause.numeric(4, 0, false)),
        new Field("TRAN-CAT-BAL",      17, PicClause.numeric(9, 2, true)),
        new Field("FILLER",            28, PicClause.text(22)),
    };

    private final String accountId;     // PIC 9(11)
    private final String typeCode;      // PIC X(2)
    private final String catCode;       // PIC 9(4)
    private final BigDecimal balance;   // PIC S9(9)V99
    private final byte[] raw;           // original 50 bytes for VSAM key extraction

    public TranCatBalRecord(String accountId, String typeCode, String catCode,
                           BigDecimal balance, byte[] raw) {
        this.accountId = accountId;
        this.typeCode = typeCode;
        this.catCode = catCode;
        this.balance = balance;
        this.raw = raw;
    }

    public String accountId()    { return accountId; }
    public String typeCode()     { return typeCode; }
    public String catCode()      { return catCode; }
    public BigDecimal balance()  { return balance; }
    public byte[] raw()          { return raw; }

    /** Composite key: accountId + typeCode + catCode (17 bytes). */
    public byte[] primaryKey() {
        byte[] key = new byte[17];
        System.arraycopy(raw, 0, key, 0, 17);
        return key;
    }

    /**
     * Decode raw bytes into a TranCatBalRecord.
     */
    public static TranCatBalRecord decode(byte[] raw) {
        if (raw.length != RECORD_LENGTH) {
            throw new IllegalArgumentException("Expected 50 bytes, got " + raw.length);
        }
        byte[] copy = Arrays.copyOf(raw, RECORD_LENGTH);

        String accountId = ZonedDecimalCodec.decode(raw, 0, 11, false).toPlainString();
        String typeCode = TextCodec.decode(raw, 11, 2);
        String catCode = ZonedDecimalCodec.decode(raw, 13, 4, false).toPlainString();
        BigDecimal balance = ZonedDecimalCodec.decode(raw, 17, 11, true);

        return new TranCatBalRecord(accountId, typeCode, catCode, balance, copy);
    }

    /**
     * Encode this record back to 50 raw bytes for WRITE/REWRITE to TCATBAL file.
     */
    public byte[] encode() {
        byte[] out = new byte[RECORD_LENGTH];

        ZonedDecimalCodec.encode(out, 0, 11, false, accountId);
        TextCodec.encode(out, 11, 2, typeCode);
        ZonedDecimalCodec.encode(out, 13, 4, false, catCode);
        ZonedDecimalCodec.encode(out, 17, 11, true, balance.toPlainString());

        // FILLER at offset 28 already zero-filled

        return out;
    }

    /**
     * Create a new TranCatBalRecord with added amount (for accumulation during posting).
     */
    public TranCatBalRecord withAddedAmount(BigDecimal delta) {
        return new TranCatBalRecord(
            this.accountId,
            this.typeCode,
            this.catCode,
            this.balance.add(delta),
            null
        );
    }

    @Override
    public String toString() {
        return "TranCatBalRecord{" +
               "accountId='" + accountId + '\'' +
               ", typeCode='" + typeCode + '\'' +
               ", catCode='" + catCode + '\'' +
               ", balance=" + balance +
               '}';
    }
}
