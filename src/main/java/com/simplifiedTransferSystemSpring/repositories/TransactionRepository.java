package com.simplifiedTransferSystemSpring.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simplifiedTransferSystemSpring.domain.transaction.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
