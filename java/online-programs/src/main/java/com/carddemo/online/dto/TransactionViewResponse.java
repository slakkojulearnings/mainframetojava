package com.carddemo.online.dto;

import java.math.BigDecimal;

public class TransactionViewResponse {
    public String tranId;
    public String cardNum;
    public String typeCode;
    public String catCode;
    public String source;
    public BigDecimal amount;
    public String desc;
    public String origTs;
    public String procTs;
    public String merchantId;
    public String merchantName;
    public String merchantCity;
    public String merchantZip;
}
