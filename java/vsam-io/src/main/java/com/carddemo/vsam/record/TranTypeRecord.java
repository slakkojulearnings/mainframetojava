package com.carddemo.vsam.record;

import com.carddemo.codec.TextCodec;

public final class TranTypeRecord {

    private static final int RECORD_LENGTH = 60;

    private final String typeCode;
    private final String typeDesc;

    public TranTypeRecord(String typeCode, String typeDesc) {
        this.typeCode = typeCode;
        this.typeDesc = typeDesc;
    }

    public String typeCode() {
        return typeCode;
    }

    public String typeDesc() {
        return typeDesc;
    }

    public static TranTypeRecord decode(byte[] raw) {
        String typeCode = TextCodec.decode(raw, 0, 2);
        String typeDesc = TextCodec.decode(raw, 2, 50);
        return new TranTypeRecord(typeCode, typeDesc);
    }
}
