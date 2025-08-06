package com.example.bankcards.entity;

import jakarta.persistence.*;

@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

}
