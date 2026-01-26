package com.simplifiedStransferSystemSpring.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.simplifiedStransferSystemSpring.domain.transaction.Transaction;
import com.simplifiedStransferSystemSpring.domain.user.User;
import com.simplifiedStransferSystemSpring.dtos.TransactionDTO;
import com.simplifiedStransferSystemSpring.repositories.TransactionRepository;

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

    public Transaction createTransaction(TransactionDTO transaction) throws Exception {

        User payer = this.userService.findUserById(transaction.payerId());
        User payee = this.userService.findUserById(transaction.payeeId());

        userService.ValidateUserTransaction(payer, transaction.value());

        boolean isAuthorized = this.autorizeTransaction(payer, transaction.value());

        if (!isAuthorized) throw new RuntimeException("Transaction not authorized");

        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setPayer(payer);
        newTransaction.setPayee(payee);
        newTransaction.setTimestamp(LocalDateTime.now());

        repository.save(newTransaction);
        payer.setBalance(payer.getBalance().subtract(transaction.value()));
        payee.setBalance(payee.getBalance().add(transaction.value()));

        repository.save(newTransaction);
        userService.saveUser(payer);
        userService.saveUser(payee);

        boolean payerNotified = this.notificationService.sendNotification(payer, "Transaction sent successfully.");
        boolean payeeNotified = this.notificationService.sendNotification(payee, "Transaction received successfully.");
        if (!payerNotified || !payeeNotified) {
            System.out.println("One or more notification deliveries failed.");
        }

        // persist notification attempt results for observability
        newTransaction.setPayerNotified(payerNotified);
        newTransaction.setPayeeNotified(payeeNotified);
        repository.save(newTransaction);

        return newTransaction;
    }

    public boolean autorizeTransaction(User payer, BigDecimal value) {
        int attempts = 0;
        int maxAttempts = 3;
        while (attempts < maxAttempts) {
            attempts++;
            try {
                ResponseEntity<Map> authorizationResponse = restTemplate
                        .getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);

                if (authorizationResponse.getStatusCode() == HttpStatus.OK && authorizationResponse.getBody() != null) {
                    Map<?, ?> body = authorizationResponse.getBody();
                    Object statusObj = body.get("status");
                    Object dataObj = body.get("data");

                    // prefer data.authorization if present
                    if (dataObj instanceof Map) {
                        Object authObj = ((Map<?, ?>) dataObj).get("authorization");
                        if (authObj instanceof Boolean) {
                            return (Boolean) authObj;
                        } else if (authObj instanceof String) {
                            return Boolean.parseBoolean((String) authObj);
                        }
                    }

                    if (statusObj instanceof String) {
                        String status = (String) statusObj;
                        if ("success".equalsIgnoreCase(status)) return true;
                        if ("fail".equalsIgnoreCase(status)) return false;
                    }

                    return false;
                } else {
                    System.out.println("Authorization endpoint returned non-OK: " + authorizationResponse.getStatusCode());
                }
            } catch (Exception e) {
                System.out.println("Authorization request failed (attempt " + attempts + "): " + e.getMessage());
            }

            try {
                Thread.sleep(200L * attempts);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    public List<Transaction> getAllTransactions() {
        return this.repository.findAll();
    }

}
