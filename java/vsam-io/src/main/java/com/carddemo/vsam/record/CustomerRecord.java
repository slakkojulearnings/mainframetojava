package com.carddemo.vsam.record;

import java.util.Arrays;
import com.carddemo.codec.Field;
import com.carddemo.codec.PicClause;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.codec.TextCodec;

/**
 * Customer record from CVCUS01Y.cpy, LRECL=500 bytes.
 *
 * <p>Customer master file. Primary key: CUST-ID (9 bytes).
 */
public final class CustomerRecord {

    public static final int RECORD_LENGTH = 500;

    public static final Field[] LAYOUT = {
        new Field("CUST-ID",                  0, PicClause.numeric(9, 0, false)),
        new Field("CUST-FIRST-NAME",          9, PicClause.text(25)),
        new Field("CUST-MIDDLE-NAME",        34, PicClause.text(25)),
        new Field("CUST-LAST-NAME",          59, PicClause.text(25)),
        new Field("CUST-ADDR-LINE-1",        84, PicClause.text(50)),
        new Field("CUST-ADDR-LINE-2",       134, PicClause.text(50)),
        new Field("CUST-ADDR-LINE-3",       184, PicClause.text(50)),
        new Field("CUST-ADDR-STATE-CD",     234, PicClause.text(2)),
        new Field("CUST-ADDR-COUNTRY-CD",   236, PicClause.text(3)),
        new Field("CUST-ADDR-ZIP",          239, PicClause.text(10)),
        new Field("CUST-PHONE-NUM-1",       249, PicClause.text(15)),
        new Field("CUST-PHONE-NUM-2",       264, PicClause.text(15)),
        new Field("CUST-SSN",               279, PicClause.numeric(9, 0, false)),
        new Field("CUST-GOVT-ISSUED-ID",   288, PicClause.text(20)),
        new Field("CUST-DOB-YYYY-MM-DD",   308, PicClause.text(10)),
        new Field("CUST-EFT-ACCOUNT-ID",   318, PicClause.text(10)),
        new Field("CUST-PRI-CARD-HOLDER-IND", 328, PicClause.text(1)),
        new Field("CUST-FICO-CREDIT-SCORE",   329, PicClause.numeric(3, 0, false)),
        new Field("FILLER",                 332, PicClause.text(168)),
    };

    private final String custId;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String addrLine1;
    private final String addrLine2;
    private final String addrLine3;
    private final String addrStateCode;
    private final String addrCountryCode;
    private final String addrZip;
    private final String phoneNum1;
    private final String phoneNum2;
    private final String ssn;
    private final String govtIssuedId;
    private final String dobYyyyMmDd;
    private final String eftAccountId;
    private final String priCardholderInd;
    private final String ficoScore;
    private final byte[] raw;

    public CustomerRecord(String custId, String firstName, String middleName, String lastName,
                         String addrLine1, String addrLine2, String addrLine3,
                         String addrStateCode, String addrCountryCode, String addrZip,
                         String phoneNum1, String phoneNum2, String ssn, String govtIssuedId,
                         String dobYyyyMmDd, String eftAccountId, String priCardholderInd,
                         String ficoScore, byte[] raw) {
        this.custId = custId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.addrLine1 = addrLine1;
        this.addrLine2 = addrLine2;
        this.addrLine3 = addrLine3;
        this.addrStateCode = addrStateCode;
        this.addrCountryCode = addrCountryCode;
        this.addrZip = addrZip;
        this.phoneNum1 = phoneNum1;
        this.phoneNum2 = phoneNum2;
        this.ssn = ssn;
        this.govtIssuedId = govtIssuedId;
        this.dobYyyyMmDd = dobYyyyMmDd;
        this.eftAccountId = eftAccountId;
        this.priCardholderInd = priCardholderInd;
        this.ficoScore = ficoScore;
        this.raw = raw;
    }

    public String custId()             { return custId; }
    public String firstName()          { return firstName; }
    public String middleName()         { return middleName; }
    public String lastName()           { return lastName; }
    public String addrLine1()          { return addrLine1; }
    public String addrLine2()          { return addrLine2; }
    public String addrLine3()          { return addrLine3; }
    public String addrStateCode()      { return addrStateCode; }
    public String addrCountryCode()    { return addrCountryCode; }
    public String addrZip()            { return addrZip; }
    public String phoneNum1()          { return phoneNum1; }
    public String phoneNum2()          { return phoneNum2; }
    public String ssn()                { return ssn; }
    public String govtIssuedId()       { return govtIssuedId; }
    public String dobYyyyMmDd()        { return dobYyyyMmDd; }
    public String eftAccountId()       { return eftAccountId; }
    public String priCardholderInd()   { return priCardholderInd; }
    public String ficoScore()          { return ficoScore; }
    public byte[] raw()                { return raw; }

    /** Primary key: custId (9 bytes). */
    public byte[] primaryKey() {
        byte[] key = new byte[9];
        System.arraycopy(raw, 0, key, 0, 9);
        return key;
    }

    /**
     * Decode raw bytes into a CustomerRecord.
     */
    public static CustomerRecord decode(byte[] raw) {
        if (raw.length != RECORD_LENGTH) {
            throw new IllegalArgumentException("Expected 500 bytes, got " + raw.length);
        }
        byte[] copy = Arrays.copyOf(raw, RECORD_LENGTH);

        String custId = ZonedDecimalCodec.decode(raw, 0, 9, false).toPlainString();
        String firstName = TextCodec.decode(raw, 9, 25);
        String middleName = TextCodec.decode(raw, 34, 25);
        String lastName = TextCodec.decode(raw, 59, 25);
        String addrLine1 = TextCodec.decode(raw, 84, 50);
        String addrLine2 = TextCodec.decode(raw, 134, 50);
        String addrLine3 = TextCodec.decode(raw, 184, 50);
        String addrStateCode = TextCodec.decode(raw, 234, 2);
        String addrCountryCode = TextCodec.decode(raw, 236, 3);
        String addrZip = TextCodec.decode(raw, 239, 10);
        String phoneNum1 = TextCodec.decode(raw, 249, 15);
        String phoneNum2 = TextCodec.decode(raw, 264, 15);
        String ssn = ZonedDecimalCodec.decode(raw, 279, 9, false).toPlainString();
        String govtIssuedId = TextCodec.decode(raw, 288, 20);
        String dobYyyyMmDd = TextCodec.decode(raw, 308, 10);
        String eftAccountId = TextCodec.decode(raw, 318, 10);
        String priCardholderInd = TextCodec.decode(raw, 328, 1);
        String ficoScore = ZonedDecimalCodec.decode(raw, 329, 3, false).toPlainString();

        return new CustomerRecord(custId, firstName, middleName, lastName,
                                addrLine1, addrLine2, addrLine3,
                                addrStateCode, addrCountryCode, addrZip,
                                phoneNum1, phoneNum2, ssn, govtIssuedId,
                                dobYyyyMmDd, eftAccountId, priCardholderInd,
                                ficoScore, copy);
    }

    @Override
    public String toString() {
        return "CustomerRecord{" +
               "custId='" + custId + '\'' +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               '}';
    }
}
