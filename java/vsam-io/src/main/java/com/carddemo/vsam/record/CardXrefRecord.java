package com.carddemo.vsam.record;

import java.util.Arrays;
import com.carddemo.codec.Field;
import com.carddemo.codec.PicClause;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.codec.TextCodec;

/**
 * Card Cross-Reference record from CVACT03Y.cpy, LRECL=50 bytes.
 *
 * <p>Primary key: cardNum (16). Alternate key: accountId (11).
 * Links cards to accounts and customers.
 */
public final class CardXrefRecord {

    public static final int RECORD_LENGTH = 50;
    public static final int ACCT_ID_OFFSET = 25;
    public static final int ACCT_ID_LENGTH = 11;

    public static final Field[] LAYOUT = {
        new Field("XREF-CARD-NUM",    0, PicClause.text(16)),
        new Field("XREF-CUST-ID",    16, PicClause.numeric(9, 0, false)),
        new Field("XREF-ACCT-ID",    25, PicClause.numeric(11, 0, false)),
        new Field("FILLER",          36, PicClause.text(14)),
    };

    private final String cardNum;       // PIC X(16)
    private final String custId;        // PIC 9(9)
    private final String accountId;     // PIC 9(11)
    private final byte[] raw;           // original 50 bytes

    public CardXrefRecord(String cardNum, String custId, String accountId, byte[] raw) {
        this.cardNum = cardNum;
        this.custId = custId;
        this.accountId = accountId;
        this.raw = raw;
    }

    public String cardNum()    { return cardNum; }
    public String custId()     { return custId; }
    public String accountId()  { return accountId; }
    public byte[] raw()        { return raw; }

    /** Primary key: cardNum (16 bytes). */
    public byte[] primaryKey() {
        byte[] key = new byte[16];
        System.arraycopy(raw, 0, key, 0, 16);
        return key;
    }

    /** Alternate key: accountId (11 bytes at offset 25). */
    public byte[] alternateKey() {
        byte[] key = new byte[11];
        System.arraycopy(raw, 25, key, 0, 11);
        return key;
    }

    public static CardXrefRecord decode(byte[] raw) {
        if (raw.length != RECORD_LENGTH) {
            throw new IllegalArgumentException("Expected 50 bytes, got " + raw.length);
        }
        byte[] copy = Arrays.copyOf(raw, RECORD_LENGTH);

        String cardNum = TextCodec.decode(raw, 0, 16);
        String custId = ZonedDecimalCodec.decode(raw, 16, 9, false).toPlainString();
        String accountId = ZonedDecimalCodec.decode(raw, 25, 11, false).toPlainString();

        return new CardXrefRecord(cardNum, custId, accountId, copy);
    }

    @Override
    public String toString() {
        return "CardXrefRecord{" +
               "cardNum='" + cardNum + '\'' +
               ", custId='" + custId + '\'' +
               ", accountId='" + accountId + '\'' +
               '}';
    }
}
