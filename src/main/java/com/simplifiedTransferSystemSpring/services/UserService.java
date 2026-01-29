package com.simplifiedTransferSystemSpring.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.simplifiedTransferSystemSpring.domain.user.User;
import com.simplifiedTransferSystemSpring.domain.user.UserType;
import com.simplifiedTransferSystemSpring.dtos.UserDTO;
import com.simplifiedTransferSystemSpring.repositories.UserRepository;


@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    public void validateUserTransaction(User payer, BigDecimal amount) {
        if (payer.getUserType().equals(UserType.MERCHANT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Merchants are not allowed to initiate transactions.");
        }

        if (payer.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds for the transaction.");
        }
    }

    public User findUserById(Long id) {
        return this.repository.findUserById(id)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
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
