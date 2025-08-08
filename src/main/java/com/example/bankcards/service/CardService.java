package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardStatusRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptor;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardStatusRepository cardStatusRepository;
    private final TransferRepository transferRepository;
    private final CardEncryptor cardEncryptor;

    public CardService(CardRepository cardRepository, UserRepository userRepository,
                       CardStatusRepository cardStatusRepository, TransferRepository transferRepository,
                       CardEncryptor cardEncryptor) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cardStatusRepository = cardStatusRepository;
        this.transferRepository = transferRepository;
        this.cardEncryptor = cardEncryptor;
    }

    @Transactional
    public Card createCard(@Valid CardDTO cardDTO) {
        logger.info("Creating card for ownerId: {}", cardDTO.getOwnerId());
        Card card = new Card();
        card.setNumber(cardEncryptor.encrypt(cardDTO.getNumber()));

        User owner = userRepository.findById(cardDTO.getOwnerId())
                .orElseThrow(() -> {
                    logger.error("User not found: {}", cardDTO.getOwnerId());
                    return new IllegalArgumentException("User not found: " + cardDTO.getOwnerId());
                });
        card.setOwner(owner);

        card.setExpiration(cardDTO.getExpiration());
        card.setBalance(cardDTO.getBalance());

        CardStatus activeStatus = cardStatusRepository.findByName("ACTIVE")
                .orElseThrow(() -> {
                    logger.error("Card status ACTIVE not found");
                    return new IllegalArgumentException("Card status ACTIVE not found");
                });
        card.setStatus(activeStatus);

        if (cardDTO.getExpiration().before(new Date())) {
            CardStatus expiredStatus = cardStatusRepository.findByName("EXPIRED")
                    .orElseThrow(() -> {
                        logger.error("Card status EXPIRED not found");
                        return new IllegalArgumentException("Card status EXPIRED not found");
                    });
            card.setStatus(expiredStatus);
        }

        Card savedCard = cardRepository.save(card);
        logger.info("Card created with ID: {}", savedCard.getId());
        return savedCard;
    }

    @Transactional(readOnly = true)
    public Page<Card> getCards(String status, String owner, Pageable pageable, String username) {
        logger.info("Fetching cards for user: {}, status: {}, owner: {}", username, status, owner);
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new IllegalArgumentException("User not found: " + username);
                });

        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        Page<Card> cards;

        if (isAdmin) {
            // ADMIN может фильтровать по статусу и владельцу
            if (status != null && owner != null) {
                User filterOwner = userRepository.findByUsername(owner)
                        .orElseThrow(() -> {
                            logger.error("User not found: {}", owner);
                            return new IllegalArgumentException("User not found: " + owner);
                        });
                CardStatus filterStatus = cardStatusRepository.findByName(status)
                        .orElseThrow(() -> {
                            logger.error("Status not found: {}", status);
                            return new IllegalArgumentException("Status not found: " + status);
                        });
                cards = cardRepository.findByStatusAndOwner(filterStatus, filterOwner, pageable);
            } else if (status != null) {
                CardStatus filterStatus = cardStatusRepository.findByName(status)
                        .orElseThrow(() -> {
                            logger.error("Status not found: {}", status);
                            return new IllegalArgumentException("Status not found: " + status);
                        });
                cards = cardRepository.findByStatus(filterStatus, pageable);
            } else if (owner != null) {
                User filterOwner = userRepository.findByUsername(owner)
                        .orElseThrow(() -> {
                            logger.error("User not found: {}", owner);
                            return new IllegalArgumentException("User not found: " + owner);
                        });
                cards = cardRepository.findByOwner(filterOwner, pageable);
            } else {
                cards = cardRepository.findAll(pageable);
            }
        } else {
            // USER видит только свои карты
            if (status != null) {
                CardStatus filterStatus = cardStatusRepository.findByName(status)
                        .orElseThrow(() -> {
                            logger.error("Status not found: {}", status);
                            return new IllegalArgumentException("Status not found: " + status);
                        });
                cards = cardRepository.findByOwnerAndStatus(currentUser, filterStatus, pageable);
            } else {
                cards = cardRepository.findByOwner(currentUser, pageable);
            }
        }

        // Маскирование номеров карт
        cards.getContent().forEach(card -> card.setNumber(cardEncryptor.maskCardNumber(card.getNumber())));
        logger.info("Returning {} cards for user: {}", cards.getTotalElements(), username);
        return cards;
    }

    @Transactional(readOnly = true)
    public Card getCardById(Long id, String username) {
        logger.info("Fetching card with ID: {} for user: {}", id, username);
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Card not found: {}", id);
                    return new IllegalArgumentException("Card not found: " + id);
                });

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new IllegalArgumentException("User not found: " + username);
                });

        if (currentUser.getRole() != User.Role.ADMIN && !card.getOwner().getId().equals(currentUser.getId())) {
            logger.error("Access denied: Card {} does not belong to user {}", id, username);
            throw new SecurityException("Access denied: Card does not belong to user");
        }

        card.setNumber(cardEncryptor.maskCardNumber(card.getNumber()));
        logger.info("Returning card with ID: {}", id);
        return card;
    }

    @Transactional
    public Card updateCard(Long id, @Valid CardDTO cardDTO) {
        logger.info("Updating card with ID: {}", id);
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Card not found: {}", id);
                    return new IllegalArgumentException("Card not found: " + id);
                });

        card.setNumber(cardEncryptor.encrypt(cardDTO.getNumber()));
        User owner = userRepository.findById(cardDTO.getOwnerId())
                .orElseThrow(() -> {
                    logger.error("User not found: {}", cardDTO.getOwnerId());
                    return new IllegalArgumentException("User not found: " + cardDTO.getOwnerId());
                });
        card.setOwner(owner);
        card.setExpiration(cardDTO.getExpiration());
        card.setBalance(cardDTO.getBalance());

        if (cardDTO.getExpiration().before(new Date())) {
            CardStatus expiredStatus = cardStatusRepository.findByName("EXPIRED")
                    .orElseThrow(() -> {
                        logger.error("Card status EXPIRED not found");
                        return new IllegalArgumentException("Card status EXPIRED not found");
                    });
            card.setStatus(expiredStatus);
        }

        Card updatedCard = cardRepository.save(card);
        logger.info("Card updated with ID: {}", id);
        return updatedCard;
    }

    @Transactional
    public void deleteCard(Long id) {
        logger.info("Deleting card with ID: {}", id);
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Card not found: {}", id);
                    return new IllegalArgumentException("Card not found: " + id);
                });
        cardRepository.delete(card);
        logger.info("Card deleted with ID: {}", id);
    }

    @Transactional
    public void transfer(@Valid TransferDTO transferDTO, String username) {
        logger.info("Initiating transfer from card {} to card {} for user: {}",
                transferDTO.getFromCardId(), transferDTO.getToCardId(), username);
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new IllegalArgumentException("User not found: " + username);
                });

        Card fromCard = cardRepository.findById(transferDTO.getFromCardId())
                .orElseThrow(() -> {
                    logger.error("From card not found: {}", transferDTO.getFromCardId());
                    return new IllegalArgumentException("From card not found: " + transferDTO.getFromCardId());
                });
        Card toCard = cardRepository.findById(transferDTO.getToCardId())
                .orElseThrow(() -> {
                    logger.error("To card not found: {}", transferDTO.getToCardId());
                    return new IllegalArgumentException("To card not found: " + transferDTO.getToCardId());
                });

        // Проверка, что обе карты принадлежат текущему пользователю
        if (!fromCard.getOwner().getId().equals(currentUser.getId()) ||
                !toCard.getOwner().getId().equals(currentUser.getId())) {
            logger.error("Access denied: Cards must belong to user {}", username);
            throw new SecurityException("Access denied: Cards must belong to the user");
        }

        // Проверка статуса карт
        CardStatus activeStatus = cardStatusRepository.findByName("ACTIVE")
                .orElseThrow(() -> {
                    logger.error("Card status ACTIVE not found");
                    return new IllegalArgumentException("Card status ACTIVE not found");
                });
        if (!fromCard.getStatus().getName().equals("ACTIVE") ||
                !toCard.getStatus().getName().equals("ACTIVE")) {
            logger.error("Transfer failed: Both cards must be active");
            throw new IllegalStateException("Both cards must be active for transfer");
        }

        // Проверка баланса
        if (fromCard.getBalance() < transferDTO.getAmount()) {
            logger.error("Transfer failed: Insufficient balance on card {}", transferDTO.getFromCardId());
            throw new IllegalStateException("Insufficient balance on source card");
        }

        // Выполнение перевода
        fromCard.setBalance(fromCard.getBalance() - transferDTO.getAmount());
        toCard.setBalance(toCard.getBalance() + transferDTO.getAmount());

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        // Сохранение записи о переводе
        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(transferDTO.getAmount());
        transferRepository.save(transfer);

        logger.info("Transfer completed from card {} to card {}",
                transferDTO.getFromCardId(), transferDTO.getToCardId());
    }

    @Transactional
    public void requestBlock(Long id, String username) {
        logger.info("Requesting block for card {} by user: {}", id, username);
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Card not found: {}", id);
                    return new IllegalArgumentException("Card not found: " + id);
                });

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new IllegalArgumentException("User not found: " + username);
                });

        if (!card.getOwner().getId().equals(currentUser.getId())) {
            logger.error("Access denied: Card {} does not belong to user {}", id, username);
            throw new SecurityException("Access denied: Card does not belong to user");
        }

        if (!card.getStatus().getName().equals("ACTIVE")) {
            logger.error("Block failed: Card {} is already blocked or expired", id);
            throw new IllegalStateException("Card is already blocked or expired");
        }

        CardStatus blockedStatus = cardStatusRepository.findByName("BLOCKED")
                .orElseThrow(() -> {
                    logger.error("Card status BLOCKED not found");
                    return new IllegalArgumentException("Card status BLOCKED not found");
                });
        card.setStatus(blockedStatus);
        cardRepository.save(card);
        logger.info("Card {} blocked successfully", id);
    }
}