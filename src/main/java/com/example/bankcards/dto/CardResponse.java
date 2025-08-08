package com.example.bankcards.dto;

import java.util.Date;

public class CardResponse {
    private Long id;
    private String cardNumber;
    private String owner;
    private Date expiryDate;
    private String status;
    private Long balance;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getBalance() { return balance; }
    public void setBalance(Long balance) { this.balance = balance; }
}


