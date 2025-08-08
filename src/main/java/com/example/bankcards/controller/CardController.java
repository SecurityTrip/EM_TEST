package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferDTO;
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

import java.security.Principal;

@RestController
@RequestMapping("/card")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> create(@Valid @RequestBody CardDTO cardDTO) {
        Card createdCard = cardService.createCard(cardDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<Card>> read(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.getCards(status, owner, pageable, principal.getName());
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Card> getById(@PathVariable Long id, Principal principal) {
        Card card = cardService.getCardById(id, principal.getName());
        return ResponseEntity.ok(card);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> update(@PathVariable Long id, @Valid @RequestBody CardDTO cardDTO) {
        Card updatedCard = cardService.updateCard(id, cardDTO);
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
}