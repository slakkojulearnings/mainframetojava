package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.BillPaymentRequest;
import com.carddemo.online.dto.BillPaymentResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.AccountRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BillPaymentController {

    @Value("${carddemo.acct.path}")
    private String acctPath;

    @PostMapping("/bill-payment")
    public ResponseEntity<?> processBillPayment(@RequestBody BillPaymentRequest req, HttpSession session) {
        try {
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            if (req.accountId == null || req.accountId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Account ID is required"));
            }

            String acctId = req.accountId.trim();
            if (!acctId.matches("^[0-9]+$") || acctId.length() != 11 || acctId.equals("00000000000")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid account number"));
            }

            if (req.paymentAmount == null || req.paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Payment amount must be greater than zero"));
            }

            Path acctFilePath = Paths.get(acctPath);

            try (KsdsReader acctReader = KsdsReader.open(acctFilePath, 300, FixedRecordReader.Mode.RAW)) {
                Optional<byte[]> acctRaw = acctReader.readByKey(acctId.getBytes());
                if (acctRaw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Account not found"));
                }

                AccountRecord acct = AccountRecord.decode(acctRaw.get());

                BigDecimal newBalance = acct.currBal().subtract(req.paymentAmount);
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse("Payment exceeds account balance"));
                }

                AccountRecord updatedAcct = new AccountRecord(
                    acct.accountId(),
                    acct.activeStatus(),
                    newBalance,
                    acct.creditLimit(),
                    acct.cashCreditLimit(),
                    acct.currCycCredit(),
                    acct.currCycDebit(),
                    acct.openDate(),
                    acct.expirationDate(),
                    acct.reissueDate(),
                    acct.groupId()
                );

                try (FixedRecordWriter writer = FixedRecordWriter.open(acctFilePath, 300)) {
                    for (byte[] record : acctReader.sequentialScan()) {
                        byte[] key = new byte[11];
                        System.arraycopy(record, 0, key, 0, 11);
                        if (new String(key).trim().equals(acctId)) {
                            writer.write(updatedAcct.encode());
                        } else {
                            writer.write(record);
                        }
                    }
                }

                BillPaymentResponse response = new BillPaymentResponse();
                response.accountId = acctId;
                response.paymentAmount = req.paymentAmount;
                response.newBalance = newBalance;
                response.confirmationNumber = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                response.processedTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

                return ResponseEntity.ok(response);
            }

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR: " + e.getMessage()));
        }
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
