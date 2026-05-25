package com.carddemo.online.dto;

import java.math.BigDecimal;

public class TransactionAddRequest {
    public String cardNum;
    public String typeCode;
    public String catCode;
    public String source;
    public BigDecimal amount;
    public String desc;
    public String merchantId;
    public String merchantName;
    public String merchantCity;
    public String merchantZip;
}
