package com.example.bankcards.dto;

public class TransferDTO {

    @jakarta.validation.constraints.NotNull
    private Long fromCardId;
    @jakarta.validation.constraints.NotNull
    private Long toCardId;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long amount;

    // Геттеры и сеттеры
    public Long getFromCardId() { return fromCardId; }
    public void setFromCardId(Long fromCardId) { this.fromCardId = fromCardId; }
    public Long getToCardId() { return toCardId; }
    public void setToCardId(Long toCardId) { this.toCardId = toCardId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
}