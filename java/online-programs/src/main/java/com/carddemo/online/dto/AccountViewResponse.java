package com.carddemo.online.dto;

import java.math.BigDecimal;

public class AccountViewResponse {
    // Account fields
    public String accountId;
    public String activeStatus;
    public BigDecimal currBal;
    public BigDecimal creditLimit;
    public BigDecimal cashCreditLimit;
    public BigDecimal currCycCredit;
    public BigDecimal currCycDebit;
    public String openDate;
    public String expirationDate;
    public String reissueDate;
    public String groupId;

    // Customer fields
    public String custId;
    public String ssn;  // formatted NNN-NN-NNNN
    public String ficoScore;
    public String dob;
    public String firstName;
    public String middleName;
    public String lastName;
    public String addrLine1;
    public String addrLine2;
    public String addrLine3;
    public String stateCode;
    public String zip;
    public String countryCode;
    public String phone1;
    public String phone2;
    public String govtId;
    public String eftAccountId;
    public String priCardHolderInd;

    // Cross-ref fields
    public String cardNum;
}
