package com.simplifiedStransferSystemSpring.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simplifiedStransferSystemSpring.domain.transaction.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
