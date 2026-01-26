package com.simplifiedStransferSystemSpring.dtos;

import java.math.BigDecimal;

import com.simplifiedStransferSystemSpring.domain.user.UserType;

public record UserDTO(
                String firstName,
                String lastName,
                String document,
                BigDecimal balance,
                String password,
                String email,
                UserType userType) {
}
