package com.example.bankcards.controller;

import com.example.bankcards.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/card")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(){
        return null;
    }

    @GetMapping("/read")
    public ResponseEntity<?> read(){
        return null;
    }

    @PatchMapping("/update")
    public ResponseEntity<?> update(){
        return null;
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(){
        return null;
    }
}
