package com.carddemo.vsam.record;

import com.carddemo.codec.TextCodec;
import com.carddemo.codec.ZonedDecimalCodec;

public final class TranCatRecord {

    private static final int RECORD_LENGTH = 60;

    private final String typeCode;
    private final String catCode;
    private final String catDesc;

    public TranCatRecord(String typeCode, String catCode, String catDesc) {
        this.typeCode = typeCode;
        this.catCode = catCode;
        this.catDesc = catDesc;
    }

    public String typeCode() {
        return typeCode;
    }

    public String catCode() {
        return catCode;
    }

    public String catDesc() {
        return catDesc;
    }

    public static TranCatRecord decode(byte[] raw) {
        String typeCode = TextCodec.decode(raw, 0, 2);
        String catCode = ZonedDecimalCodec.decode(raw, 2, 4, false);
        String catDesc = TextCodec.decode(raw, 6, 50);
        return new TranCatRecord(typeCode, catCode, catDesc);
    }
}
