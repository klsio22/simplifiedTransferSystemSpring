package com.simplifiedStransferSystemSpring.services;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;

import com.simplifiedStransferSystemSpring.domain.user.User;
import com.simplifiedStransferSystemSpring.domain.user.UserType;
import com.simplifiedStransferSystemSpring.repositories.UserRepository;

public class UserService {

    @Autowired
    private UserRepository repository;

    public void ValidateUserTransaction(User payer, BigDecimal amount) throws Exception {
        if (payer.getUserType().equals(UserType.MERCHANT)) {
            throw new IllegalArgumentException("Merchants are not allowed to initiate transactions.");
        }

        if (payer.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for the transaction.");
        }
    }

    public User findUserById(Long id) throws Exception {
        return this.repository.findUserById(id)
                .orElseThrow(() -> new Exception("User not found with id: " + id));
    }

    public void saveUser(User user) {
        this.repository.save(user);
    }

}
