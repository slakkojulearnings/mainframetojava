package com.carddemo.vsam.record;

import java.math.BigDecimal;
import java.util.Arrays;
import com.carddemo.codec.Field;
import com.carddemo.codec.PicClause;
import com.carddemo.codec.ZonedDecimalCodec;
import com.carddemo.codec.TextCodec;

/**
 * Account Master record from CVACT01Y.cpy, LRECL=300 bytes.
 *
 * <p>Key: accountId (11). Used for account balance updates with interest accrual.
 */
public final class AccountRecord {

    public static final int RECORD_LENGTH = 300;

    public static final Field[] LAYOUT = {
        new Field("ACCT-ID",                   0, PicClause.numeric(11, 0, false)),
        new Field("ACCT-ACTIVE-STATUS",       11, PicClause.text(1)),
        new Field("ACCT-CURR-BAL",            12, PicClause.numeric(10, 2, true)),
        new Field("ACCT-CREDIT-LIMIT",        24, PicClause.numeric(10, 2, true)),
        new Field("ACCT-CASH-CREDIT-LIMIT",   36, PicClause.numeric(10, 2, true)),
        new Field("ACCT-OPEN-DATE",           48, PicClause.text(10)),
        new Field("ACCT-EXPIRAION-DATE",      58, PicClause.text(10)),
        new Field("ACCT-REISSUE-DATE",        68, PicClause.text(10)),
        new Field("ACCT-CURR-CYC-CREDIT",     78, PicClause.numeric(10, 2, true)),
        new Field("ACCT-CURR-CYC-DEBIT",      90, PicClause.numeric(10, 2, true)),
        new Field("ACCT-ADDR-ZIP",           102, PicClause.text(10)),
        new Field("ACCT-GROUP-ID",           112, PicClause.text(10)),
        new Field("FILLER",                  122, PicClause.text(178)),
    };

    private final String accountId;
    private final String activeStatus;
    private final BigDecimal currBal;
    private final BigDecimal creditLimit;
    private final BigDecimal cashCreditLimit;
    private final String openDate;
    private final String expirationDate;
    private final String reissueDate;
    private final BigDecimal currCycCredit;
    private final BigDecimal currCycDebit;
    private final String addrZip;
    private final String groupId;
    private final byte[] raw;

    public AccountRecord(String accountId, String activeStatus, BigDecimal currBal,
                       BigDecimal creditLimit, BigDecimal cashCreditLimit,
                       String openDate, String expirationDate, String reissueDate,
                       BigDecimal currCycCredit, BigDecimal currCycDebit,
                       String addrZip, String groupId, byte[] raw) {
        this.accountId = accountId;
        this.activeStatus = activeStatus;
        this.currBal = currBal;
        this.creditLimit = creditLimit;
        this.cashCreditLimit = cashCreditLimit;
        this.openDate = openDate;
        this.expirationDate = expirationDate;
        this.reissueDate = reissueDate;
        this.currCycCredit = currCycCredit;
        this.currCycDebit = currCycDebit;
        this.addrZip = addrZip;
        this.groupId = groupId;
        this.raw = raw;
    }

    public String accountId()           { return accountId; }
    public String activeStatus()        { return activeStatus; }
    public BigDecimal currBal()         { return currBal; }
    public BigDecimal creditLimit()     { return creditLimit; }
    public BigDecimal cashCreditLimit() { return cashCreditLimit; }
    public String openDate()            { return openDate; }
    public String expirationDate()      { return expirationDate; }
    public String reissueDate()         { return reissueDate; }
    public BigDecimal currCycCredit()   { return currCycCredit; }
    public BigDecimal currCycDebit()    { return currCycDebit; }
    public String addrZip()             { return addrZip; }
    public String groupId()             { return groupId; }
    public byte[] raw()                 { return raw; }

    /** Primary key: accountId (11 bytes). */
    public byte[] primaryKey() {
        byte[] key = new byte[11];
        System.arraycopy(raw, 0, key, 0, 11);
        return key;
    }

    public static AccountRecord decode(byte[] raw) {
        if (raw.length != RECORD_LENGTH) {
            throw new IllegalArgumentException("Expected 300 bytes, got " + raw.length);
        }
        byte[] copy = Arrays.copyOf(raw, RECORD_LENGTH);

        String accountId = ZonedDecimalCodec.decode(raw, 0, 11, false).toPlainString();
        String activeStatus = TextCodec.decode(raw, 11, 1);
        BigDecimal currBal = ZonedDecimalCodec.decode(raw, 12, 12, true);
        BigDecimal creditLimit = ZonedDecimalCodec.decode(raw, 24, 12, true);
        BigDecimal cashCreditLimit = ZonedDecimalCodec.decode(raw, 36, 12, true);
        String openDate = TextCodec.decode(raw, 48, 10);
        String expirationDate = TextCodec.decode(raw, 58, 10);
        String reissueDate = TextCodec.decode(raw, 68, 10);
        BigDecimal currCycCredit = ZonedDecimalCodec.decode(raw, 78, 12, true);
        BigDecimal currCycDebit = ZonedDecimalCodec.decode(raw, 90, 12, true);
        String addrZip = TextCodec.decode(raw, 102, 10);
        String groupId = TextCodec.decode(raw, 112, 10);

        return new AccountRecord(accountId, activeStatus, currBal, creditLimit, cashCreditLimit,
                               openDate, expirationDate, reissueDate,
                               currCycCredit, currCycDebit, addrZip, groupId, copy);
    }

    /**
     * Create a new AccountRecord with updated balance and zeroed cycle fields.
     * Used for REWRITE after interest accrual.
     */
    public AccountRecord withInterestAccrued(BigDecimal totalInterest) {
        return new AccountRecord(
            this.accountId,
            this.activeStatus,
            this.currBal.add(totalInterest),
            this.creditLimit,
            this.cashCreditLimit,
            this.openDate,
            this.expirationDate,
            this.reissueDate,
            BigDecimal.ZERO,  // zeroed cycle credit
            BigDecimal.ZERO,  // zeroed cycle debit
            this.addrZip,
            this.groupId,
            null  // raw will be re-encoded on write
        );
    }

    /**
     * Encode this record back to 300 raw bytes for VSAM REWRITE.
     */
    public byte[] encode() {
        byte[] out = new byte[RECORD_LENGTH];

        ZonedDecimalCodec.encode(out, 0, 11, false, accountId);
        TextCodec.encode(out, 11, 1, activeStatus);
        ZonedDecimalCodec.encode(out, 12, 12, true, currBal);
        ZonedDecimalCodec.encode(out, 24, 12, true, creditLimit);
        ZonedDecimalCodec.encode(out, 36, 12, true, cashCreditLimit);
        TextCodec.encode(out, 48, 10, openDate);
        TextCodec.encode(out, 58, 10, expirationDate);
        TextCodec.encode(out, 68, 10, reissueDate);
        ZonedDecimalCodec.encode(out, 78, 12, true, currCycCredit);
        ZonedDecimalCodec.encode(out, 90, 12, true, currCycDebit);
        TextCodec.encode(out, 102, 10, addrZip);
        TextCodec.encode(out, 112, 10, groupId);

        // FILLER at offset 122 is already zero-filled

        return out;
    }

    @Override
    public String toString() {
        return "AccountRecord{" +
               "accountId='" + accountId + '\'' +
               ", currBal=" + currBal +
               ", groupId='" + groupId + '\'' +
               '}';
    }
}
