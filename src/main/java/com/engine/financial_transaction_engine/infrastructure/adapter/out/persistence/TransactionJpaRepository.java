package com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, UUID> {
    Optional<TransactionJpaEntity> findByIdempotencyKey(String idempotencyKey);
}