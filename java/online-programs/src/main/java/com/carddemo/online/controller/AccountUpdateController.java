package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.AccountUpdateRequest;
import com.carddemo.online.dto.AccountUpdateResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.AccountRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AccountUpdateController {

    @Value("${carddemo.acct.path}")
    private String acctPath;

    @PostMapping("/account/{acctId}/update")
    public ResponseEntity<?> updateAccount(@PathVariable String acctId, @RequestBody AccountUpdateRequest req, HttpSession session) {
        try {
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            acctId = acctId.trim();
            if (acctId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Account number not provided"));
            }
            if (!acctId.matches("^[0-9]+$")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Account Filter must be a non-zero 11 digit number"));
            }
            if (acctId.length() != 11 || acctId.equals("00000000000")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Account Filter must be a non-zero 11 digit number"));
            }

            if (req.activeStatus == null || req.creditLimit == null || req.cashCreditLimit == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("activeStatus, creditLimit, and cashCreditLimit are required"));
            }

            Path acctFilePath = Paths.get(acctPath);

            try (KsdsReader acctReader = KsdsReader.open(acctFilePath, 300, FixedRecordReader.Mode.RAW)) {
                Optional<byte[]> acctRaw = acctReader.readByKey(acctId.getBytes());
                if (acctRaw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Account: " + acctId + " not found in Acct Master file."));
                }

                AccountRecord acct = AccountRecord.decode(acctRaw.get());
                acct = new AccountRecord(
                    acct.accountId(),
                    req.activeStatus,
                    acct.currBal(),
                    req.creditLimit,
                    req.cashCreditLimit,
                    acct.currCycCredit(),
                    acct.currCycDebit(),
                    acct.openDate(),
                    acct.expirationDate(),
                    acct.reissueDate(),
                    acct.groupId()
                );

                // For demo: write updated record back
                try (FixedRecordWriter writer = FixedRecordWriter.open(acctFilePath, 300)) {
                    for (byte[] record : acctReader.sequentialScan()) {
                        byte[] key = new byte[11];
                        System.arraycopy(record, 0, key, 0, 11);
                        if (new String(key).trim().equals(acctId)) {
                            writer.write(acct.encode());
                        } else {
                            writer.write(record);
                        }
                    }
                }

                AccountUpdateResponse response = new AccountUpdateResponse();
                response.accountId = acct.accountId();
                response.activeStatus = acct.activeStatus();
                response.creditLimit = acct.creditLimit();
                response.cashCreditLimit = acct.cashCreditLimit();

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
