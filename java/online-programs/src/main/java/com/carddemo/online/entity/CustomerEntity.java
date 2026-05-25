package com.carddemo.online.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
public class CustomerEntity {
    @Id
    private String custId;
    private String ssn;
    private String ficoScore;
    private String dob;
    private String firstName;
    private String middleName;
    private String lastName;
    private String addrLine1;
    private String addrLine2;
    private String addrLine3;
    private String addrStateCode;
    private String addrZip;
    private String addrCountryCode;
    private String phone1;
    private String phone2;
    private String govtId;
    private String eftAccountId;
    private String priCardHolderInd;

    public CustomerEntity() {}

    public String getCustId() { return custId; }
    public void setCustId(String custId) { this.custId = custId; }
    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }
    public String getFicoScore() { return ficoScore; }
    public void setFicoScore(String ficoScore) { this.ficoScore = ficoScore; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getAddrLine1() { return addrLine1; }
    public void setAddrLine1(String addrLine1) { this.addrLine1 = addrLine1; }
    public String getAddrLine2() { return addrLine2; }
    public void setAddrLine2(String addrLine2) { this.addrLine2 = addrLine2; }
    public String getAddrLine3() { return addrLine3; }
    public void setAddrLine3(String addrLine3) { this.addrLine3 = addrLine3; }
    public String getAddrStateCode() { return addrStateCode; }
    public void setAddrStateCode(String addrStateCode) { this.addrStateCode = addrStateCode; }
    public String getAddrZip() { return addrZip; }
    public void setAddrZip(String addrZip) { this.addrZip = addrZip; }
    public String getAddrCountryCode() { return addrCountryCode; }
    public void setAddrCountryCode(String addrCountryCode) { this.addrCountryCode = addrCountryCode; }
    public String getPhone1() { return phone1; }
    public void setPhone1(String phone1) { this.phone1 = phone1; }
    public String getPhone2() { return phone2; }
    public void setPhone2(String phone2) { this.phone2 = phone2; }
    public String getGovtId() { return govtId; }
    public void setGovtId(String govtId) { this.govtId = govtId; }
    public String getEftAccountId() { return eftAccountId; }
    public void setEftAccountId(String eftAccountId) { this.eftAccountId = eftAccountId; }
    public String getPriCardHolderInd() { return priCardHolderInd; }
    public void setPriCardHolderInd(String priCardHolderInd) { this.priCardHolderInd = priCardHolderInd; }
}
