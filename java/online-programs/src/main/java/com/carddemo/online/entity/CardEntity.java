package com.carddemo.online.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "card")
public class CardEntity {
    @Id
    private String cardNum;
    private String accountId;
    private String embossedName;
    private String activeStatus;
    private String expirationDate;

    public CardEntity() {}

    public CardEntity(String cardNum, String accountId, String embossedName, String activeStatus, String expirationDate) {
        this.cardNum = cardNum;
        this.accountId = accountId;
        this.embossedName = embossedName;
        this.activeStatus = activeStatus;
        this.expirationDate = expirationDate;
    }

    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getEmbossedName() { return embossedName; }
    public void setEmbossedName(String embossedName) { this.embossedName = embossedName; }
    public String getActiveStatus() { return activeStatus; }
    public void setActiveStatus(String activeStatus) { this.activeStatus = activeStatus; }
    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
}
