package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.TransactionViewResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.KsdsReader;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TransactionViewController {

    @Value("${carddemo.tran.path}")
    private String tranPath;

    @GetMapping("/transaction/{tranId}")
    public ResponseEntity<?> getTransactionView(@PathVariable String tranId, HttpSession session) {
        try {
            // Validate session
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            // Validate tranId
            tranId = tranId.trim();
            if (tranId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Tran ID can NOT be empty and must be 16 characters"));
            }
            if (tranId.length() != 16) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Tran ID can NOT be empty and must be 16 characters"));
            }

            Path tranFilePath = Paths.get(tranPath);

            try (KsdsReader tranReader = KsdsReader.open(tranFilePath, 350, FixedRecordReader.Mode.RAW)) {
                Optional<byte[]> tranRaw = tranReader.readByKey(tranId.getBytes());
                if (tranRaw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Transaction ID NOT found in Transaction master."));
                }

                TransactionRecord tran = TransactionRecord.decode(tranRaw.get());

                TransactionViewResponse response = new TransactionViewResponse();
                response.tranId = tran.tranId();
                response.cardNum = tran.cardNum();
                response.typeCode = tran.typeCode();
                response.catCode = tran.catCode();
                response.source = tran.source();
                response.amount = tran.amount();
                response.desc = tran.desc();
                response.origTs = tran.origTs();
                response.procTs = tran.procTs();
                response.merchantId = tran.merchantId();
                response.merchantName = tran.merchantName();
                response.merchantCity = tran.merchantCity();
                response.merchantZip = tran.merchantZip();

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
