package com.simplifiedTransferSystemSpring.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.simplifiedTransferSystemSpring.domain.transaction.Transaction;
import com.simplifiedTransferSystemSpring.domain.user.User;
import com.simplifiedTransferSystemSpring.dtos.TransactionDTO;
import com.simplifiedTransferSystemSpring.repositories.TransactionRepository;

@Service
public class TransactionService {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private NotificationsService notificationService;

    @Autowired
    private RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private User loadUser(Long id) {
        return this.userService.findUserById(id);
    }

    private void validateTransaction(User payer, BigDecimal amount) {
        this.userService.ValidateUserTransaction(payer, amount);
    }

    private Transaction buildTransaction(TransactionDTO dto, User payer, User payee) {
        Transaction t = new Transaction();
        t.setAmount(dto.value());
        t.setPayer(payer);
        t.setPayee(payee);
        t.setTimestamp(LocalDateTime.now());
        return t;
    }

    private void updateBalancesAndSave(User payer, User payee, BigDecimal amount) {
        payer.setBalance(payer.getBalance().subtract(amount));
        payee.setBalance(payee.getBalance().add(amount));
        userService.saveUser(payer);
        userService.saveUser(payee);
    }

    @Transactional
    public Transaction createTransaction(TransactionDTO transaction) {
        User payer = loadUser(transaction.payerId());
        User payee = loadUser(transaction.payeeId());

        validateTransaction(payer, transaction.value());

        validateAuthorization();

        Transaction newTransaction = executeTransaction(transaction, payer, payee);

        return newTransaction;
    }

    private void validateAuthorization() {
        boolean isAuthorized = authorizeTransaction();
        if (!isAuthorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transaction not authorized");
        }
    }

    private Transaction executeTransaction(TransactionDTO dto, User payer, User payee) {
        Transaction newTransaction = buildTransaction(dto, payer, payee);
        updateBalancesAndSave(payer, payee, dto.value());

        Transaction saved = repository.save(newTransaction);

        boolean payerNotified = notificationService.sendNotification(payer, "Transaction sent successfully.");
        boolean payeeNotified = notificationService.sendNotification(payee, "Transaction received successfully.");

        try {
            saved.setPayerNotified(payerNotified);
            saved.setPayeeNotified(payeeNotified);
            repository.saveAndFlush(saved);
        } catch (Exception e) {
            logger.warn("Failed to persist notification flags for transaction {}: {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    public boolean authorizeTransaction() {
        int attempts = 0;
        int maxAttempts = 3;
        while (attempts < maxAttempts) {
            attempts++;
            try {
                logger.debug("Authorization attempt {} (no payer/amount passed)", attempts);
                ResponseEntity<Map<String, Object>> authorizationResponse = restTemplate.exchange(
                        "https://util.devi.tools/api/v2/authorize",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        });

                if (authorizationResponse.getStatusCode() == HttpStatus.OK && authorizationResponse.getBody() != null) {
                    Boolean parsed = parseAuthorizationResponse(authorizationResponse.getBody());
                    if (parsed != null)
                        return parsed;
                    return false;
                }
            } catch (RestClientException e) {
                logger.warn("Authorization request failed (attempt {}): {}", attempts, e.getMessage());
            }
        }
        return false;
    }

    private Boolean parseAuthorizationResponse(Map<String, Object> body) {
        Object dataObj = body.get("data");
        Object statusObj = body.get("status");

        if (dataObj instanceof Map<?, ?> dataMap) {
            Object authObj = dataMap.get("authorization");
            if (authObj instanceof Boolean authBoolean)
                return authBoolean;
            if (authObj instanceof String authString)
                return Boolean.valueOf(authString);
            return false;
        }

        if (statusObj instanceof String status) {
            return switch (status.toLowerCase()) {
                case "success" -> true;
                case "fail" -> false;
                default -> false;
            };
        }

        return null;
    }

    public List<Transaction> getAllTransactions() {
        return this.repository.findAll();
    }

}
