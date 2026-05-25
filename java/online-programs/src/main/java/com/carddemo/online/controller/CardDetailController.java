package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.CardDetailResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.CardRecord;

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
public class CardDetailController {

    @Value("${carddemo.card.path}")
    private String cardPath;

    @GetMapping("/card/{cardNum}")
    public ResponseEntity<?> getCardDetail(@PathVariable String cardNum, HttpSession session) {
        try {
            // Validate session
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            // Validate cardNum
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

            Path cardFilePath = Paths.get(cardPath);

            try (KsdsReader cardReader = KsdsReader.open(cardFilePath, 150, FixedRecordReader.Mode.RAW)) {
                Optional<byte[]> cardRaw = cardReader.readByKey(cardNum.getBytes());
                if (cardRaw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("Did not find cards for this search condition"));
                }

                CardRecord card = CardRecord.decode(cardRaw.get());

                CardDetailResponse response = new CardDetailResponse();
                response.acctId = card.accountId();
                response.embossedName = card.embossedName();
                response.expirationDate = card.expirationDate();
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
