package com.carddemo.online.dto;

import java.math.BigDecimal;
import java.util.List;

public class AccountStatementResponse {
    public String accountId;
    public String custName;
    public BigDecimal currentBalance;
    public BigDecimal totalDebits;
    public BigDecimal totalCredits;
    public int transactionCount;
    public List<TransactionItem> recentTransactions;

    public static class TransactionItem {
        public String tranId;
        public String amount;
        public String date;
        public String desc;
    }
}
