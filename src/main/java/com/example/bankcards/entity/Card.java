package com.example.bankcards.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.util.Set;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", nullable = false, unique = true)
    private String number; // Зашифрованный номер

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Temporal(TemporalType.DATE)
    @Column(name = "expiration", nullable = false)
    private Date expiration;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private CardStatus status;

    @Column(name = "balance", nullable = false)
    private Long balance = 0L;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }
}
