package com.carddemo.online.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction")
public class TransactionEntity {
    @Id
    private String tranId;
    private String cardNum;
    private String typeCode;
    private String catCode;
    private String source;
    private BigDecimal amount;
    private String desc;
    private String origTs;
    private String procTs;
    private String merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;

    public TransactionEntity() {}

    public TransactionEntity(String tranId, String cardNum, String typeCode, String catCode,
                           String source, BigDecimal amount, String desc, String origTs,
                           String procTs, String merchantId, String merchantName, String merchantCity, String merchantZip) {
        this.tranId = tranId;
        this.cardNum = cardNum;
        this.typeCode = typeCode;
        this.catCode = catCode;
        this.source = source;
        this.amount = amount;
        this.desc = desc;
        this.origTs = origTs;
        this.procTs = procTs;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCity = merchantCity;
        this.merchantZip = merchantZip;
    }

    public String getTranId() { return tranId; }
    public void setTranId(String tranId) { this.tranId = tranId; }
    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getCatCode() { return catCode; }
    public void setCatCode(String catCode) { this.catCode = catCode; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public String getOrigTs() { return origTs; }
    public void setOrigTs(String origTs) { this.origTs = origTs; }
    public String getProcTs() { return procTs; }
    public void setProcTs(String procTs) { this.procTs = procTs; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getMerchantCity() { return merchantCity; }
    public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }
    public String getMerchantZip() { return merchantZip; }
    public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }
}
