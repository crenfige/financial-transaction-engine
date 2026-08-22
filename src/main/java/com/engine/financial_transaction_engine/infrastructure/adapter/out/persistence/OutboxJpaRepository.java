package com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxEntity, UUID> {
    List<OutboxEntity> findTop50ByStatusOrderByCreatedAtAsc(OutboxEntity.OutboxStatus status);
}