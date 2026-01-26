package com.simplifiedTransferSystemSpring.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.simplifiedTransferSystemSpring.domain.user.User;
import com.simplifiedTransferSystemSpring.domain.user.UserType;
import com.simplifiedTransferSystemSpring.dtos.UserDTO;
import com.simplifiedTransferSystemSpring.repositories.UserRepository;

@Service
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
