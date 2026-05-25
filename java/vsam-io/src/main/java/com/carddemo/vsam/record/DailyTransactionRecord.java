package com.carddemo.vsam.record;

import java.math.BigDecimal;
import java.util.Arrays;
import com.carddemo.codec.Field;
import com.carddemo.codec.PicClause;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.codec.TextCodec;

/**
 * Daily Transaction record from CVTRA06Y.cpy, LRECL=350 bytes.
 *
 * <p>Input daily transaction file (DALYTRAN-FILE). Same field layout as TransactionRecord
 * (CVTRA05Y) but distinct copybook and type — represents raw input transactions before posting.
 */
public final class DailyTransactionRecord {

    public static final int RECORD_LENGTH = 350;

    public static final Field[] LAYOUT = {
        new Field("DALYTRAN-ID",               0, PicClause.text(16)),
        new Field("DALYTRAN-TYPE-CD",         16, PicClause.text(2)),
        new Field("DALYTRAN-CAT-CD",          18, PicClause.numeric(4, 0, false)),
        new Field("DALYTRAN-SOURCE",          22, PicClause.text(10)),
        new Field("DALYTRAN-DESC",            32, PicClause.text(100)),
        new Field("DALYTRAN-AMT",            132, PicClause.numeric(9, 2, true)),
        new Field("DALYTRAN-MERCHANT-ID",    143, PicClause.numeric(9, 0, false)),
        new Field("DALYTRAN-MERCHANT-NAME",  152, PicClause.text(50)),
        new Field("DALYTRAN-MERCHANT-CITY",  202, PicClause.text(50)),
        new Field("DALYTRAN-MERCHANT-ZIP",   252, PicClause.text(10)),
        new Field("DALYTRAN-CARD-NUM",       262, PicClause.text(16)),
        new Field("DALYTRAN-ORIG-TS",        278, PicClause.text(26)),
        new Field("DALYTRAN-PROC-TS",        304, PicClause.text(26)),
        new Field("FILLER",                  330, PicClause.text(20)),
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
    private final byte[] raw;

    public DailyTransactionRecord(String tranId, String typeCode, String catCode,
                                String source, String desc, BigDecimal amount,
                                String merchantId, String merchantName, String merchantCity,
                                String merchantZip, String cardNum, String origTs,
                                String procTs, byte[] raw) {
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
        this.raw = raw;
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
    public byte[] raw()            { return raw; }

    /**
     * Decode raw bytes into a DailyTransactionRecord.
     */
    public static DailyTransactionRecord decode(byte[] raw) {
        if (raw.length != RECORD_LENGTH) {
            throw new IllegalArgumentException("Expected 350 bytes, got " + raw.length);
        }
        byte[] copy = Arrays.copyOf(raw, RECORD_LENGTH);

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

        return new DailyTransactionRecord(tranId, typeCode, catCode, source, desc, amount,
                                        merchantId, merchantName, merchantCity, merchantZip,
                                        cardNum, origTs, procTs, copy);
    }

    @Override
    public String toString() {
        return "DailyTransactionRecord{" +
               "tranId='" + tranId + '\'' +
               ", typeCode='" + typeCode + '\'' +
               ", amount=" + amount +
               ", cardNum='" + cardNum + '\'' +
               '}';
    }
}
