package com.carddemo.online.dto;

import java.math.BigDecimal;
import java.util.List;

public class TransactionListResponse {
    public List<TransactionItem> transactions;

    public static class TransactionItem {
        public String tranId;
        public String cardNum;
        public String typeCode;
        public BigDecimal amount;
        public String procTs;
    }
}
