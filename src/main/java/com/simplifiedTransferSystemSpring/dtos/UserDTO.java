package com.simplifiedTransferSystemSpring.dtos;

import java.math.BigDecimal;

import com.simplifiedTransferSystemSpring.domain.user.UserType;

public record UserDTO(
                String firstName,
                String lastName,
                String document,
                BigDecimal balance,
                String password,
                String email,
                UserType userType) {
}
