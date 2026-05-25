package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.CardListResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.KsdsReader;
import com.carddemo.vsam.record.CardRecord;

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
public class CardListController {

    @Value("${carddemo.card.path}")
    private String cardPath;

    @GetMapping("/cards")
    public ResponseEntity<?> listCards(@RequestParam(required = false) String accountId, HttpSession session) {
        try {
            CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");
            if (commarea == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Not authenticated"));
            }

            Path cardFilePath = Paths.get(cardPath);

            try (KsdsReader cardReader = KsdsReader.open(cardFilePath, 150, FixedRecordReader.Mode.RAW)) {
                List<CardListResponse.CardItem> cards = new ArrayList<>();

                for (byte[] record : cardReader.sequentialScan()) {
                    CardRecord card = CardRecord.decode(record);

                    if (accountId != null && !accountId.isEmpty()) {
                        if (!card.accountId().trim().equals(accountId.trim())) {
                            continue;
                        }
                    }

                    CardListResponse.CardItem item = new CardListResponse.CardItem();
                    item.cardNum = card.cardNum();
                    item.accountId = card.accountId();
                    item.embossedName = card.embossedName();
                    item.activeStatus = card.activeStatus();
                    item.expirationDate = card.expirationDate();
                    cards.add(item);
                }

                CardListResponse response = new CardListResponse();
                response.cards = cards;

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
