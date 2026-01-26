package com.simplifiedStransferSystemSpring.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.simplifiedStransferSystemSpring.domain.user.User;
import com.simplifiedStransferSystemSpring.domain.user.UserType;
import com.simplifiedStransferSystemSpring.dtos.UserDTO;
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

    public User createUser(UserDTO data) {
        User newUser = new User(data);
        this.saveUser(newUser);
        return newUser;
    }

    public List<User> getAllUsers() {
        return this.repository.findAll();
    }

    public void saveUser(User user) {
        this.repository.save(user);
    }

}
