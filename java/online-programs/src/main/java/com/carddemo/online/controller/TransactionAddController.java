package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.TransactionAddRequest;
import com.carddemo.online.dto.TransactionAddResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.TransactionRecord;

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

@RestController
@RequestMapping("/api")
public class TransactionAddController {

    @Value("${carddemo.tran.path}")
    private String tranPath;

    @PostMapping("/transaction/add")
    public ResponseEntity<?> addTransaction(@RequestBody TransactionAddRequest req, HttpSession session) {
        try {
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            if (req.cardNum == null || req.cardNum.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Card number is required"));
            }
            if (!req.cardNum.matches("^[0-9]+$")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Card number must be numeric"));
            }
            if (req.amount == null || req.amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Amount must be greater than zero"));
            }

            String tranId = generateTranId();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String procTs = sdf.format(new Date());

            TransactionRecord tran = new TransactionRecord(
                tranId,
                req.cardNum,
                req.typeCode != null ? req.typeCode : "PUR",
                req.catCode != null ? req.catCode : "0000",
                req.source != null ? req.source : "POS",
                req.amount,
                req.desc != null ? req.desc : "",
                procTs,
                procTs,
                req.merchantId != null ? req.merchantId : "",
                req.merchantName != null ? req.merchantName : "",
                req.merchantCity != null ? req.merchantCity : "",
                req.merchantZip != null ? req.merchantZip : ""
            );

            Path tranFilePath = Paths.get(tranPath);

            try (KsdsReader tranReader = KsdsReader.open(tranFilePath, 350, FixedRecordReader.Mode.RAW)) {
                try (FixedRecordWriter writer = FixedRecordWriter.open(tranFilePath, 350)) {
                    for (byte[] record : tranReader.sequentialScan()) {
                        writer.write(record);
                    }
                    writer.write(tran.encode());
                }
            }

            TransactionAddResponse response = new TransactionAddResponse();
            response.tranId = tranId;
            response.cardNum = req.cardNum;
            response.amount = req.amount;
            response.procTs = procTs;

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR: " + e.getMessage()));
        }
    }

    private String generateTranId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        int sequence = (int)(System.nanoTime() % 100);
        return String.format("%s%02d", timestamp, sequence);
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
