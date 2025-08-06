package com.example.bankcards.entity;

import jakarta.persistence.*;
@Entity

public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

}
