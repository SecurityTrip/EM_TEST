package com.example.bankcards.util;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;

public class CardMapper {
    public static CardResponse toResponse(Card card) {
        CardResponse resp = new CardResponse();
        resp.setId(card.getId());
        resp.setCardNumber(card.getNumber());
        resp.setOwner(card.getOwner().getUsername());
        resp.setExpiryDate(card.getExpiration());
        resp.setStatus(card.getStatus().getName());
        resp.setBalance(card.getBalance());
        return resp;
    }

    public static CardResponse toResponse(Card card, String maskedNumber) {
        CardResponse resp = toResponse(card);
        resp.setCardNumber(maskedNumber);
        return resp;
    }
}


