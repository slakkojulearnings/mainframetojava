package com.carddemo.vsam.record;

import java.util.Arrays;
import com.carddemo.codec.Field;
import com.carddemo.codec.PicClause;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.codec.TextCodec;

/**
 * Card record from CVACT02Y.cpy, LRECL=150 bytes.
 *
 * <p>Card master file. Primary key: CARD-NUM (16 bytes).
 */
public final class CardRecord {

    public static final int RECORD_LENGTH = 150;

    public static final Field[] LAYOUT = {
        new Field("CARD-NUM",                0, PicClause.text(16)),
        new Field("CARD-ACCT-ID",           16, PicClause.numeric(11, 0, false)),
        new Field("CARD-CVV-CD",            27, PicClause.numeric(3, 0, false)),
        new Field("CARD-EMBOSSED-NAME",    30, PicClause.text(50)),
        new Field("CARD-EXPIRAION-DATE",   80, PicClause.text(10)),
        new Field("CARD-ACTIVE-STATUS",    90, PicClause.text(1)),
        new Field("FILLER",                91, PicClause.text(59)),
    };

    private final String cardNum;
    private final String accountId;
    private final String cvvCode;
    private final String embossedName;
    private final String expirationDate;
    private final String activeStatus;
    private final byte[] raw;

    public CardRecord(String cardNum, String accountId, String cvvCode, String embossedName,
                     String expirationDate, String activeStatus, byte[] raw) {
        this.cardNum = cardNum;
        this.accountId = accountId;
        this.cvvCode = cvvCode;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
        this.raw = raw;
    }

    public String cardNum()          { return cardNum; }
    public String accountId()        { return accountId; }
    public String cvvCode()          { return cvvCode; }
    public String embossedName()     { return embossedName; }
    public String expirationDate()   { return expirationDate; }
    public String activeStatus()     { return activeStatus; }
    public byte[] raw()              { return raw; }

    /** Primary key: cardNum (16 bytes). */
    public byte[] primaryKey() {
        byte[] key = new byte[16];
        System.arraycopy(raw, 0, key, 0, 16);
        return key;
    }

    /**
     * Decode raw bytes into a CardRecord.
     */
    public static CardRecord decode(byte[] raw) {
        if (raw.length != RECORD_LENGTH) {
            throw new IllegalArgumentException("Expected 150 bytes, got " + raw.length);
        }
        byte[] copy = Arrays.copyOf(raw, RECORD_LENGTH);

        String cardNum = TextCodec.decode(raw, 0, 16);
        String accountId = ZonedDecimalCodec.decode(raw, 16, 11, false).toPlainString();
        String cvvCode = ZonedDecimalCodec.decode(raw, 27, 3, false).toPlainString();
        String embossedName = TextCodec.decode(raw, 30, 50);
        String expirationDate = TextCodec.decode(raw, 80, 10);
        String activeStatus = TextCodec.decode(raw, 90, 1);

        return new CardRecord(cardNum, accountId, cvvCode, embossedName,
                            expirationDate, activeStatus, copy);
    }

    @Override
    public String toString() {
        return "CardRecord{" +
               "cardNum='" + cardNum + '\'' +
               ", accountId='" + accountId + '\'' +
               ", activeStatus='" + activeStatus + '\'' +
               '}';
    }
}
