package com.simplifiedStransferSystemSpring.dtos;

import java.math.BigDecimal;

public record UserDTO(String fistName, String lastName, String document, BigDecimal balance, String password,
        String email, com.simplifiedStransferSystemSpring.domain.user.UserType userType) {

}
