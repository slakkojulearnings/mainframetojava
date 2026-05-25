package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.TransactionListResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.TransactionRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TransactionListController {

    @Value("${carddemo.tran.path}")
    private String tranPath;

    @GetMapping("/transactions")
    public ResponseEntity<?> listTransactions(@RequestParam(required = false) String cardNum, HttpSession session) {
        try {
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            Path tranFilePath = Paths.get(tranPath);

            try (KsdsReader tranReader = KsdsReader.open(tranFilePath, 350, FixedRecordReader.Mode.RAW)) {
                List<TransactionListResponse.TransactionItem> transactions = new ArrayList<>();

                for (byte[] record : tranReader.sequentialScan()) {
                    TransactionRecord tran = TransactionRecord.decode(record);

                    if (cardNum != null && !cardNum.isEmpty()) {
                        if (!tran.cardNum().trim().equals(cardNum.trim())) {
                            continue;
                        }
                    }

                    TransactionListResponse.TransactionItem item = new TransactionListResponse.TransactionItem();
                    item.tranId = tran.tranId();
                    item.cardNum = tran.cardNum();
                    item.typeCode = tran.typeCode();
                    item.amount = tran.amount();
                    item.procTs = tran.procTs();
                    transactions.add(item);
                }

                TransactionListResponse response = new TransactionListResponse();
                response.transactions = transactions;

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
