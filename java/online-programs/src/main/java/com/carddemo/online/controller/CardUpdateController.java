package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.CardUpdateRequest;
import com.carddemo.online.dto.CardUpdateResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.FixedRecordWriter;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.CardRecord;

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
public class CardUpdateController {

    @Value("${carddemo.card.path}")
    private String cardPath;

    @PostMapping("/card/{cardNum}/update")
    public ResponseEntity<?> updateCard(@PathVariable String cardNum, @RequestBody CardUpdateRequest req, HttpSession session) {
        try {
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            cardNum = cardNum.trim();
            if (cardNum.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Card number is required"));
            }
            if (!cardNum.matches("^[0-9]+$")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Card number must be numeric"));
            }
            if (cardNum.equals("0000000000000000")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Card number cannot be zero"));
            }

            if (req.activeStatus == null || req.activeStatus.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("activeStatus is required"));
            }

            Path cardFilePath = Paths.get(cardPath);

            try (KsdsReader cardReader = KsdsReader.open(cardFilePath, 150, FixedRecordReader.Mode.RAW)) {
                Optional<byte[]> cardRaw = cardReader.readByKey(cardNum.getBytes());
                if (cardRaw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Did not find cards for this search condition"));
                }

                CardRecord card = CardRecord.decode(cardRaw.get());
                card = new CardRecord(
                    card.cardNum(),
                    card.accountId(),
                    card.embossedName(),
                    req.activeStatus,
                    card.expirationDate()
                );

                try (FixedRecordWriter writer = FixedRecordWriter.open(cardFilePath, 150)) {
                    for (byte[] record : cardReader.sequentialScan()) {
                        byte[] key = new byte[16];
                        System.arraycopy(record, 0, key, 0, 16);
                        if (new String(key).trim().equals(cardNum)) {
                            writer.write(card.encode());
                        } else {
                            writer.write(record);
                        }
                    }
                }

                CardUpdateResponse response = new CardUpdateResponse();
                response.cardNum = card.cardNum();
                response.activeStatus = card.activeStatus();

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
