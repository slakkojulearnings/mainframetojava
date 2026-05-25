package com.carddemo.online;

public class CardDemoCommarea {

    // CDEMO-GENERAL-INFO
    public String fromTranId;        // X(04)
    public String fromProgram;       // X(08)
    public String toTranId;          // X(04)
    public String toProgram;         // X(08)
    public String userId;            // X(08)
    public String userType;          // X(01): 'A' or 'U'
    public int pgmContext;           // 9(01): 0=enter, 1=reenter

    // CDEMO-CUSTOMER-INFO
    public String custId;            // 9(09)
    public String custFname;         // X(25)
    public String custMname;         // X(25)
    public String custLname;         // X(25)

    // CDEMO-ACCOUNT-INFO
    public String acctId;            // 9(11)
    public String acctStatus;        // X(01)

    // CDEMO-CARD-INFO
    public String cardNum;           // 9(16)

    // CDEMO-MORE-INFO
    public String lastMap;           // X(07)
    public String lastMapset;        // X(07)

    public CardDemoCommarea() {
    }

    public boolean isAdmin() {
        return "A".equals(userType);
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (custFname != null && !custFname.isBlank()) {
            sb.append(custFname.trim());
        }
        if (custMname != null && !custMname.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(custMname.trim());
        }
        if (custLname != null && !custLname.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(custLname.trim());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "CardDemoCommarea{" +
               "userId='" + userId + '\'' +
               ", userType='" + userType + '\'' +
               ", custId='" + custId + '\'' +
               ", acctId='" + acctId + '\'' +
               '}';
    }
}
