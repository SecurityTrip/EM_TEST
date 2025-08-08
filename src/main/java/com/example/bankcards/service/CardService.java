package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.CardResponse;
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
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_EXPIRED = "EXPIRED";

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
    public CardResponse createCard(@Valid CardDTO cardDTO) {
        logger.info("Creating card for ownerId: {}", cardDTO.getOwnerId());
        Card card = new Card();
        card.setNumber(cardEncryptor.encrypt(cardDTO.getNumber()));
        card.setOwner(requireUserById(cardDTO.getOwnerId()));
        card.setExpiration(cardDTO.getExpiration());
        card.setBalance(cardDTO.getBalance());

        // Установка статуса
        CardStatus targetStatus = requireStatus(STATUS_ACTIVE);
        if (cardDTO.getExpiration().before(new Date())) {
            targetStatus = requireStatus(STATUS_EXPIRED);
        }
        card.setStatus(targetStatus);

        Card savedCard = cardRepository.save(card);
        // Маскируем только для ответа, не перетирая значение в сущности
        String masked = cardEncryptor.maskCardNumber(savedCard.getNumber());
        logger.info("Card created with ID: {}", savedCard.getId());
        return com.example.bankcards.util.CardMapper.toResponse(savedCard, masked);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCards(String status, String owner, Pageable pageable, String username) {
        logger.info("Fetching cards for user: {}, status: {}, owner: {}", username, status, owner);
        User currentUser = requireUserByUsername(username);

        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        Page<Card> cards;

        if (isAdmin) {
            // ADMIN может фильтровать по статусу и владельцу
            if (status != null && owner != null) {
                User filterOwner = requireUserByUsername(owner);
                CardStatus filterStatus = requireStatus(status);
                cards = cardRepository.findByStatusAndOwner(filterStatus, filterOwner, pageable);
            } else if (status != null) {
                CardStatus filterStatus = requireStatus(status);
                cards = cardRepository.findByStatus(filterStatus, pageable);
            } else if (owner != null) {
                User filterOwner = requireUserByUsername(owner);
                cards = cardRepository.findByOwner(filterOwner, pageable);
            } else {
                cards = cardRepository.findAll(pageable);
            }
        } else {
            // USER видит только свои карты
            if (status != null) {
                CardStatus filterStatus = requireStatus(status);
                cards = cardRepository.findByOwnerAndStatus(currentUser, filterStatus, pageable);
            } else {
                cards = cardRepository.findByOwner(currentUser, pageable);
            }
        }

        // Маскирование номеров карт
        logger.info("Returning {} cards for user: {}", cards.getTotalElements(), username);
        return cards.map(c -> com.example.bankcards.util.CardMapper.toResponse(c, cardEncryptor.maskCardNumber(c.getNumber())));
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long id, String username) {
        logger.info("Fetching card with ID: {} for user: {}", id, username);
        Card card = requireCardById(id);

        User currentUser = requireUserByUsername(username);

        if (currentUser.getRole() != User.Role.ADMIN && !card.getOwner().getId().equals(currentUser.getId())) {
            logger.error("Access denied: Card {} does not belong to user {}", id, username);
            throw new SecurityException("Access denied: Card does not belong to user");
        }

        String masked = cardEncryptor.maskCardNumber(card.getNumber());
        logger.info("Returning card with ID: {}", id);
        return com.example.bankcards.util.CardMapper.toResponse(card, masked);
    }

    @Transactional
    public CardResponse updateCard(Long id, @Valid CardDTO cardDTO) {
        logger.info("Updating card with ID: {}", id);
        Card card = requireCardById(id);

        card.setNumber(cardEncryptor.encrypt(cardDTO.getNumber()));
        User owner = requireUserById(cardDTO.getOwnerId());
        card.setOwner(owner);
        card.setExpiration(cardDTO.getExpiration());
        card.setBalance(cardDTO.getBalance());

        if (cardDTO.getExpiration().before(new Date())) {
            CardStatus expiredStatus = requireStatus(STATUS_EXPIRED);
            card.setStatus(expiredStatus);
        }

        Card updatedCard = cardRepository.save(card);
        String masked = cardEncryptor.maskCardNumber(updatedCard.getNumber());
        logger.info("Card updated with ID: {}", id);
        return com.example.bankcards.util.CardMapper.toResponse(updatedCard, masked);
    }

    @Transactional
    public void deleteCard(Long id) {
        logger.info("Deleting card with ID: {}", id);
        Card card = requireCardById(id);
        cardRepository.delete(card);
        logger.info("Card deleted with ID: {}", id);
    }

    @Transactional
    public void transfer(@Valid TransferDTO transferDTO, String username) {
        logger.info("Initiating transfer from card {} to card {} for user: {}",
                transferDTO.getFromCardId(), transferDTO.getToCardId(), username);
        User currentUser = requireUserByUsername(username);

        Card fromCard = requireCardById(transferDTO.getFromCardId());
        Card toCard = requireCardById(transferDTO.getToCardId());

        // Проверка, что обе карты принадлежат текущему пользователю
        if (!fromCard.getOwner().getId().equals(currentUser.getId()) ||
                !toCard.getOwner().getId().equals(currentUser.getId())) {
            logger.error("Access denied: Cards must belong to user {}", username);
            throw new SecurityException("Access denied: Cards must belong to the user");
        }

        // Проверка статуса карт
        CardStatus activeStatus = requireStatus(STATUS_ACTIVE);
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
        Card card = requireCardById(id);

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new IllegalArgumentException("User not found: " + username);
                });

        if (!card.getOwner().getId().equals(currentUser.getId())) {
            logger.error("Access denied: Card {} does not belong to user {}", id, username);
            throw new SecurityException("Access denied: Card does not belong to user");
        }

        if (!card.getStatus().getName().equals(STATUS_ACTIVE)) {
            logger.error("Block failed: Card {} is already blocked or expired", id);
            throw new IllegalStateException("Card is already blocked or expired");
        }

        CardStatus blockedStatus = requireStatus(STATUS_BLOCKED);
        card.setStatus(blockedStatus);
        cardRepository.save(card);
        logger.info("Card {} blocked successfully", id);
    }

    @Transactional
    public void blockCardAdmin(Long id) {
        logger.info("Admin blocking card {}", id);
        Card card = requireCardById(id);
        CardStatus blockedStatus = requireStatus(STATUS_BLOCKED);
        card.setStatus(blockedStatus);
        cardRepository.save(card);
        logger.info("Card {} blocked by admin", id);
    }

    @Transactional
    public void activateCardAdmin(Long id) {
        logger.info("Admin activating card {}", id);
        Card card = requireCardById(id);
        CardStatus activeStatus = requireStatus(STATUS_ACTIVE);
        card.setStatus(activeStatus);
        cardRepository.save(card);
        logger.info("Card {} activated by admin", id);
    }

    // Helpers
    private User requireUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found by username: {}", username);
                    return new IllegalArgumentException("User not found: " + username);
                });
    }

    private User requireUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found by id: {}", userId);
                    return new IllegalArgumentException("User not found: " + userId);
                });
    }

    private Card requireCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    logger.error("Card not found: {}", cardId);
                    return new IllegalArgumentException("Card not found: " + cardId);
                });
    }

    private CardStatus requireStatus(String statusName) {
        return cardStatusRepository.findByName(statusName)
                .orElseThrow(() -> {
                    logger.error("Status not found: {}", statusName);
                    return new IllegalArgumentException("Status not found: " + statusName);
                });
    }
}