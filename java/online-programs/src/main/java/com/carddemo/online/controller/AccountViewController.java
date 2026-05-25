package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.AccountViewResponse;
import com.carddemo.vsam.AixReader;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.CustomerRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AccountViewController {

    @Value("${carddemo.xref.path}")
    private String xrefPath;

    @Value("${carddemo.acct.path}")
    private String acctPath;

    @Value("${carddemo.cust.path}")
    private String custPath;

    @GetMapping("/account/{acctId}")
    public ResponseEntity<?> getAccountView(@PathVariable String acctId, HttpSession session) {
        try {
            // Validate session
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            // Validate acctId
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

            Path xrefFilePath = Paths.get(xrefPath);
            Path acctFilePath = Paths.get(acctPath);
            Path custFilePath = Paths.get(custPath);

            // Step 1: Read xref AIX by account ID (keyOffset=25, keyLength=11)
            try (KsdsReader xrefReader = KsdsReader.open(xrefFilePath, 50, FixedRecordReader.Mode.RAW);
                 KsdsReader acctReader = KsdsReader.open(acctFilePath, 300, FixedRecordReader.Mode.RAW);
                 KsdsReader custReader = KsdsReader.open(custFilePath, 500, FixedRecordReader.Mode.RAW)) {

                AixReader xrefAix = new AixReader(xrefReader, 25, 11);
                Optional<byte[]> xrefRaw = xrefAix.readByAlternateKey(acctId.getBytes());
                if (xrefRaw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Account: " + acctId + " not found in Cross ref file."));
                }

                CardXrefRecord xref = CardXrefRecord.decode(xrefRaw.get());
                String custId = xref.custId().trim();
                String cardNum = xref.cardNum().trim();

                // Step 2: Read account master by account ID
                Optional<byte[]> acctRaw = acctReader.readByKey(acctId.getBytes());
                if (acctRaw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Account: " + acctId + " not found in Acct Master file."));
                }

                AccountRecord acct = AccountRecord.decode(acctRaw.get());

                // Step 3: Read customer master by customer ID
                byte[] custIdKey = formatZonedDecimalKey(custId, 9);
                Optional<byte[]> custRaw = custReader.readByKey(custIdKey);
                if (custRaw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("CustId: " + custId + " not found in customer master."));
                }

                CustomerRecord cust = CustomerRecord.decode(custRaw.get());

                // Build response
                AccountViewResponse response = new AccountViewResponse();

                // Account fields
                response.accountId = acct.accountId();
                response.activeStatus = acct.activeStatus();
                response.currBal = acct.currBal();
                response.creditLimit = acct.creditLimit();
                response.cashCreditLimit = acct.cashCreditLimit();
                response.currCycCredit = acct.currCycCredit();
                response.currCycDebit = acct.currCycDebit();
                response.openDate = acct.openDate();
                response.expirationDate = acct.expirationDate();
                response.reissueDate = acct.reissueDate();
                response.groupId = acct.groupId();

                // Customer fields
                response.custId = cust.custId();
                response.ssn = formatSSN(cust.ssn());
                response.ficoScore = cust.ficoScore();
                response.dob = cust.dob();
                response.firstName = cust.firstName();
                response.middleName = cust.middleName();
                response.lastName = cust.lastName();
                response.addrLine1 = cust.addrLine1();
                response.addrLine2 = cust.addrLine2();
                response.addrLine3 = cust.addrLine3();
                response.stateCode = cust.addrStateCode();
                response.zip = cust.addrZip();
                response.countryCode = cust.addrCountryCode();
                response.phone1 = cust.phone1();
                response.phone2 = cust.phone2();
                response.govtId = cust.govtId();
                response.eftAccountId = cust.eftAccountId();
                response.priCardHolderInd = cust.priCardHolderInd();

                // Cross-ref fields
                response.cardNum = cardNum;

                return ResponseEntity.ok(response);

            }

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR: " + e.getMessage()));
        }
    }

    private String formatSSN(String ssn) {
        if (ssn == null || ssn.length() < 9) {
            return ssn;
        }
        return String.format("%s-%s-%s", ssn.substring(0, 3), ssn.substring(3, 5), ssn.substring(5, 9));
    }

    private byte[] formatZonedDecimalKey(String value, int length) {
        // Pad with zeros on the left to match zoned decimal format
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
