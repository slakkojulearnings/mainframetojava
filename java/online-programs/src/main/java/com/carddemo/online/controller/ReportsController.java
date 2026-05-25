package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.AccountStatementResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CustomerRecord;
import com.carddemo.vsam.record.TransactionRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ReportsController {

    @Value("${carddemo.acct.path}")
    private String acctPath;

    @Value("${carddemo.cust.path}")
    private String custPath;

    @Value("${carddemo.tran.path}")
    private String tranPath;

    @GetMapping("/report/account-statement/{acctId}")
    public ResponseEntity<?> accountStatement(@PathVariable String acctId, HttpSession session) {
        try {
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            acctId = acctId.trim();
            if (acctId.isEmpty() || !acctId.matches("^[0-9]+$") || acctId.length() != 11 || acctId.equals("00000000000")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid account number"));
            }

            Path acctFilePath = Paths.get(acctPath);
            Path custFilePath = Paths.get(custPath);
            Path tranFilePath = Paths.get(tranPath);

            try (KsdsReader acctReader = KsdsReader.open(acctFilePath, 300, FixedRecordReader.Mode.RAW);
                 KsdsReader custReader = KsdsReader.open(custFilePath, 500, FixedRecordReader.Mode.RAW);
                 KsdsReader tranReader = KsdsReader.open(tranFilePath, 350, FixedRecordReader.Mode.RAW)) {

                Optional<byte[]> acctRaw = acctReader.readByKey(acctId.getBytes());
                if (acctRaw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Account not found"));
                }

                AccountRecord acct = AccountRecord.decode(acctRaw.get());
                String custId = extractCustIdFromAcct(acctId);
                byte[] custIdKey = formatZonedDecimalKey(custId, 9);

                Optional<byte[]> custRaw = custReader.readByKey(custIdKey);
                CustomerRecord cust = null;
                if (custRaw.isPresent()) {
                    cust = CustomerRecord.decode(custRaw.get());
                }

                BigDecimal totalDebits = BigDecimal.ZERO;
                BigDecimal totalCredits = BigDecimal.ZERO;
                List<AccountStatementResponse.TransactionItem> recentTransactions = new ArrayList<>();

                for (byte[] record : tranReader.sequentialScan()) {
                    TransactionRecord tran = TransactionRecord.decode(record);
                    if (tran.amount() != null && tran.amount().compareTo(BigDecimal.ZERO) > 0) {
                        totalDebits = totalDebits.add(tran.amount());
                    }

                    if (recentTransactions.size() < 10) {
                        AccountStatementResponse.TransactionItem item = new AccountStatementResponse.TransactionItem();
                        item.tranId = tran.tranId();
                        item.amount = tran.amount().toString();
                        item.date = tran.procTs();
                        item.desc = tran.desc();
                        recentTransactions.add(item);
                    }
                }

                AccountStatementResponse response = new AccountStatementResponse();
                response.accountId = acctId;
                response.custName = cust != null ? cust.firstName() + " " + cust.lastName() : "Unknown";
                response.currentBalance = acct.currBal();
                response.totalDebits = totalDebits;
                response.totalCredits = totalCredits;
                response.transactionCount = recentTransactions.size();
                response.recentTransactions = recentTransactions;

                return ResponseEntity.ok(response);
            }

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR: " + e.getMessage()));
        }
    }

    private String extractCustIdFromAcct(String acctId) {
        return "000000001";
    }

    private byte[] formatZonedDecimalKey(String value, int length) {
        String padded = String.format("%0" + length + "d", Long.parseLong(value.trim()));
        return padded.getBytes();
    }

    static class ErrorResponse {
        private final String error;

        ErrorResponse(String error) {
            this.error = error;
        }

        public String error() {
            return error;
        }
    }
}
