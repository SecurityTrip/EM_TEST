package com.example.bankcards.dto;

import java.util.Date;

public class CardDTO {

    private String number;
    private Long ownerId;
    private Date expiration;
    private Long balance;

    // Геттеры и сеттеры
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Date getExpiration() { return expiration; }
    public void setExpiration(Date expiration) { this.expiration = expiration; }
    public Long getBalance() { return balance; }
    public void setBalance(Long balance) { this.balance = balance; }
}