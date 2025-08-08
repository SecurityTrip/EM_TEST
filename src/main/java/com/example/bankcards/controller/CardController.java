package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;

@RestController
@RequestMapping("/card")
@Tag(name = "Cards", description = "Operations with cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CardDTO cardDTO) {
        CardResponse createdCard = cardService.createCard(cardDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<CardResponse>> read(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardResponse> cards = cardService.getCards(status, owner, pageable, principal.getName());
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponse> getById(@PathVariable Long id, Principal principal) {
        CardResponse card = cardService.getCardById(id, principal.getName());
        return ResponseEntity.ok(card);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> update(@PathVariable Long id, @Valid @RequestBody CardDTO cardDTO) {
        CardResponse updatedCard = cardService.updateCard(id, cardDTO);
        return ResponseEntity.ok(updatedCard);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferDTO transferDTO, Principal principal) {
        cardService.transfer(transferDTO, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> requestBlock(@PathVariable Long id, Principal principal) {
        cardService.requestBlock(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/block-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> blockByAdmin(@PathVariable Long id) {
        cardService.blockCardAdmin(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateByAdmin(@PathVariable Long id) {
        cardService.activateCardAdmin(id);
        return ResponseEntity.ok().build();
    }
}