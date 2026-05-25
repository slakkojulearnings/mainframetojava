package com.carddemo.online.dto;

import java.util.List;

public class CardListResponse {
    public List<CardItem> cards;

    public static class CardItem {
        public String cardNum;
        public String accountId;
        public String embossedName;
        public String activeStatus;
        public String expirationDate;
    }
}
