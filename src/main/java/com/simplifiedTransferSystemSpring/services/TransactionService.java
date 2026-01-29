package com.simplifiedTransferSystemSpring.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

    private User loadUser(Long id) throws Exception {
        return this.userService.findUserById(id);
    }

    private void validateTransaction(User payer, BigDecimal amount) throws Exception {
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
    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        User payer = loadUser(transaction.payerId());
        User payee = loadUser(transaction.payeeId());

        validateTransaction(payer, transaction.value());

        boolean isAuthorized = authorizeTransaction(payer, transaction.value());
        if (!isAuthorized)
            throw new RuntimeException("Transaction not authorized");

        Transaction newTransaction = buildTransaction(transaction, payer, payee);

        updateBalancesAndSave(payer, payee, transaction.value());

        repository.save(newTransaction);

        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        boolean payerNotified = notificationService.sendNotification(payer,
                                "Transaction sent successfully.");
                        boolean payeeNotified = notificationService.sendNotification(payee,
                                "Transaction received successfully.");
                        if (!payerNotified || !payeeNotified) {
                            logger.warn("One or more notification deliveries failed.");
                        }
                        newTransaction.setPayerNotified(payerNotified);
                        newTransaction.setPayeeNotified(payeeNotified);
                        try {
                            repository.save(newTransaction);
                        } catch (Exception e) {
                            logger.warn("Failed to persist notification flags for transaction {}: {}",
                                    newTransaction.getId(), e.getMessage());
                        }
                    }

                    // no-op implementations for other lifecycle methods
                    @Override
                    public void beforeCommit(boolean readOnly) {
                    }

                    @Override
                    public void beforeCompletion() {
                    }

                    @Override
                    public void afterCompletion(int status) {
                    }

                    @Override
                    public void flush() {
                    }

                    @Override
                    public void suspend() {
                    }

                    @Override
                    public void resume() {
                    }
                });

        return newTransaction;
    }

    public boolean authorizeTransaction(User payer, BigDecimal value) {
        int attempts = 0;
        int maxAttempts = 3;
        while (attempts < maxAttempts) {
            attempts++;
            try {
                ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity(
                        "https://util.devi.tools/api/v2/authorize?payerId={payerId}&amount={amount}",
                        Map.class, payer.getId(), value.toPlainString());

                if (authorizationResponse.getStatusCode() == HttpStatus.OK && authorizationResponse.getBody() != null) {
                    Boolean parsed = parseAuthorizationResponse(authorizationResponse.getBody());
                    if (parsed != null)
                        return parsed;
                    return false;
                } else {
                    logger.warn("Authorization endpoint returned non-OK (attempt {}): {}", attempts,
                            authorizationResponse.getStatusCode());
                }
            } catch (RestClientException e) {
                logger.warn("Authorization request failed (attempt {}): {}", attempts, e.getMessage());
            }
        }
        return false;
    }

    private Boolean parseAuthorizationResponse(Map<?, ?> body) {
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
