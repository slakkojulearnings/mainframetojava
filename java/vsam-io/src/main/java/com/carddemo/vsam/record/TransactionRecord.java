package com.carddemo.vsam.record;

import java.math.BigDecimal;
import com.carddemo.codec.Field;
import com.carddemo.codec.PicClause;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.codec.TextCodec;

/**
 * Transaction record from CVTRA05Y.cpy, LRECL=350 bytes.
 *
 * <p>Output record written sequentially for interest transactions and batch posting.
 * Used as the TRAN-RECORD output from CBACT04C.
 */
public final class TransactionRecord {

    public static final int RECORD_LENGTH = 350;

    public static final Field[] LAYOUT = {
        new Field("TRAN-ID",               0, PicClause.text(16)),
        new Field("TRAN-TYPE-CD",         16, PicClause.text(2)),
        new Field("TRAN-CAT-CD",          18, PicClause.numeric(4, 0, false)),
        new Field("TRAN-SOURCE",          22, PicClause.text(10)),
        new Field("TRAN-DESC",            32, PicClause.text(100)),
        new Field("TRAN-AMT",            132, PicClause.numeric(9, 2, true)),
        new Field("TRAN-MERCHANT-ID",    143, PicClause.numeric(9, 0, false)),
        new Field("TRAN-MERCHANT-NAME",  152, PicClause.text(50)),
        new Field("TRAN-MERCHANT-CITY",  202, PicClause.text(50)),
        new Field("TRAN-MERCHANT-ZIP",   252, PicClause.text(10)),
        new Field("TRAN-CARD-NUM",       262, PicClause.text(16)),
        new Field("TRAN-ORIG-TS",        278, PicClause.text(26)),
        new Field("TRAN-PROC-TS",        304, PicClause.text(26)),
        new Field("FILLER",              330, PicClause.text(20)),
    };

    private final String tranId;
    private final String typeCode;
    private final String catCode;
    private final String source;
    private final String desc;
    private final BigDecimal amount;
    private final String merchantId;
    private final String merchantName;
    private final String merchantCity;
    private final String merchantZip;
    private final String cardNum;
    private final String origTs;
    private final String procTs;

    public TransactionRecord(String tranId, String typeCode, String catCode,
                           String source, String desc, BigDecimal amount,
                           String merchantId, String merchantName, String merchantCity,
                           String merchantZip, String cardNum, String origTs, String procTs) {
        this.tranId = tranId;
        this.typeCode = typeCode;
        this.catCode = catCode;
        this.source = source;
        this.desc = desc;
        this.amount = amount;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCity = merchantCity;
        this.merchantZip = merchantZip;
        this.cardNum = cardNum;
        this.origTs = origTs;
        this.procTs = procTs;
    }

    public String tranId()         { return tranId; }
    public String typeCode()       { return typeCode; }
    public String catCode()        { return catCode; }
    public String source()         { return source; }
    public String desc()           { return desc; }
    public BigDecimal amount()     { return amount; }
    public String merchantId()     { return merchantId; }
    public String merchantName()   { return merchantName; }
    public String merchantCity()   { return merchantCity; }
    public String merchantZip()    { return merchantZip; }
    public String cardNum()        { return cardNum; }
    public String origTs()         { return origTs; }
    public String procTs()         { return procTs; }

    /**
     * Encode this transaction into 350 raw bytes for sequential write.
     */
    public byte[] encode() {
        byte[] out = new byte[RECORD_LENGTH];

        TextCodec.encode(out, 0, 16, tranId);
        TextCodec.encode(out, 16, 2, typeCode);
        ZonedDecimalCodec.encode(out, 18, 4, false, catCode);
        TextCodec.encode(out, 22, 10, source);
        TextCodec.encode(out, 32, 100, desc);
        ZonedDecimalCodec.encode(out, 132, 11, true, amount);
        ZonedDecimalCodec.encode(out, 143, 9, false, merchantId);
        TextCodec.encode(out, 152, 50, merchantName);
        TextCodec.encode(out, 202, 50, merchantCity);
        TextCodec.encode(out, 252, 10, merchantZip);
        TextCodec.encode(out, 262, 16, cardNum);
        TextCodec.encode(out, 278, 26, origTs);
        TextCodec.encode(out, 304, 26, procTs);

        // FILLER at offset 330 already zero-filled

        return out;
    }

    /**
     * Decode raw bytes into a TransactionRecord (for reading existing transactions).
     */
    public static TransactionRecord decode(byte[] raw) {
        if (raw.length != RECORD_LENGTH) {
            throw new IllegalArgumentException("Expected 350 bytes, got " + raw.length);
        }

        String tranId = TextCodec.decode(raw, 0, 16);
        String typeCode = TextCodec.decode(raw, 16, 2);
        String catCode = ZonedDecimalCodec.decode(raw, 18, 4, false).toPlainString();
        String source = TextCodec.decode(raw, 22, 10);
        String desc = TextCodec.decode(raw, 32, 100);
        BigDecimal amount = ZonedDecimalCodec.decode(raw, 132, 11, true);
        String merchantId = ZonedDecimalCodec.decode(raw, 143, 9, false).toPlainString();
        String merchantName = TextCodec.decode(raw, 152, 50);
        String merchantCity = TextCodec.decode(raw, 202, 50);
        String merchantZip = TextCodec.decode(raw, 252, 10);
        String cardNum = TextCodec.decode(raw, 262, 16);
        String origTs = TextCodec.decode(raw, 278, 26);
        String procTs = TextCodec.decode(raw, 304, 26);

        return new TransactionRecord(tranId, typeCode, catCode, source, desc, amount,
                                   merchantId, merchantName, merchantCity, merchantZip,
                                   cardNum, origTs, procTs);
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
               "tranId='" + tranId + '\'' +
               ", typeCode='" + typeCode + '\'' +
               ", amount=" + amount +
               ", cardNum='" + cardNum + '\'' +
               '}';
    }
}
