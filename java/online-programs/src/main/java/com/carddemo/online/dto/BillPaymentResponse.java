package com.carddemo.online.dto;

import java.math.BigDecimal;

public class BillPaymentResponse {
    public String accountId;
    public BigDecimal paymentAmount;
    public BigDecimal newBalance;
    public String confirmationNumber;
    public String processedTime;
}
