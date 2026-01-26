package com.simplifiedStransferSystemSpring.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

        this.notificationService.sendNotification(payer, "Transaction sent successfully.");
        this.notificationService.sendNotification(payee, "Transaction received successfully.");

        return newTransaction;
    }

    public boolean autorizeTransaction(User payer, BigDecimal value) {

    
        ResponseEntity<Map> authorizationResponse = restTemplate
                .getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);

        if (authorizationResponse.getStatusCode() == HttpStatus.OK) {
            String message = (String) authorizationResponse.getBody().get("message");
            return "Autorizado".equalsIgnoreCase(message);
        } else
            return false;

    }

}
