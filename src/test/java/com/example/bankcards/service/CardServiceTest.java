package com.example.bankcards.service;

import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardStatusRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class CardServiceTest {

    private CardRepository cardRepository;
    private UserRepository userRepository;
    private CardStatusRepository cardStatusRepository;
    private TransferRepository transferRepository;
    private CardEncryptor cardEncryptor;
    private CardService cardService;

    @BeforeEach
    void setup() {
        cardRepository = Mockito.mock(CardRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        cardStatusRepository = Mockito.mock(CardStatusRepository.class);
        transferRepository = Mockito.mock(TransferRepository.class);
        cardEncryptor = Mockito.mock(CardEncryptor.class);
        cardService = new CardService(cardRepository, userRepository, cardStatusRepository, transferRepository, cardEncryptor);
    }

    @Test
    void transfer_success_updatesBalancesAndSavesTransfer() {
        String username = "u1";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);

        Card from = new Card();
        from.setId(10L);
        from.setOwner(user);
        from.setBalance(1000L);
        CardStatus active = new CardStatus();
        active.setName("ACTIVE");
        from.setStatus(active);

        Card to = new Card();
        to.setId(20L);
        to.setOwner(user);
        to.setBalance(100L);
        to.setStatus(active);

        Mockito.when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        Mockito.when(cardRepository.findById(10L)).thenReturn(Optional.of(from));
        Mockito.when(cardRepository.findById(20L)).thenReturn(Optional.of(to));
        Mockito.when(cardStatusRepository.findByName("ACTIVE")).thenReturn(Optional.of(active));

        TransferDTO dto = new TransferDTO();
        dto.setFromCardId(10L);
        dto.setToCardId(20L);
        dto.setAmount(300L);

        cardService.transfer(dto, username);

        Assertions.assertEquals(700L, from.getBalance());
        Assertions.assertEquals(400L, to.getBalance());
        Mockito.verify(transferRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    void transfer_fails_whenInsufficientBalance() {
        String username = "u1";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);

        Card from = new Card();
        from.setId(10L);
        from.setOwner(user);
        from.setBalance(100L);
        CardStatus active = new CardStatus();
        active.setName("ACTIVE");
        from.setStatus(active);

        Card to = new Card();
        to.setId(20L);
        to.setOwner(user);
        to.setBalance(100L);
        to.setStatus(active);

        Mockito.when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        Mockito.when(cardRepository.findById(10L)).thenReturn(Optional.of(from));
        Mockito.when(cardRepository.findById(20L)).thenReturn(Optional.of(to));
        Mockito.when(cardStatusRepository.findByName("ACTIVE")).thenReturn(Optional.of(active));

        TransferDTO dto = new TransferDTO();
        dto.setFromCardId(10L);
        dto.setToCardId(20L);
        dto.setAmount(300L);

        Assertions.assertThrows(IllegalStateException.class, () -> cardService.transfer(dto, username));
        Mockito.verify(transferRepository, Mockito.never()).save(Mockito.any());
    }
}


