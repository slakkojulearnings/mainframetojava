package com.carddemo.online.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "card_xref")
public class CardXrefEntity {
    @Id
    private String cardNum;
    private String custId;
    private String accountId;

    public CardXrefEntity() {}

    public CardXrefEntity(String cardNum, String custId, String accountId) {
        this.cardNum = cardNum;
        this.custId = custId;
        this.accountId = accountId;
    }

    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }
    public String getCustId() { return custId; }
    public void setCustId(String custId) { this.custId = custId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
}
