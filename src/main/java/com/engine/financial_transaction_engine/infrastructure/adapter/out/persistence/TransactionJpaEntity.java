package com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence;

import com.engine.financial_transaction_engine.domain.model.Transaction;
import com.engine.financial_transaction_engine.domain.model.TransactionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TransactionJpaEntity() {}

    public static TransactionJpaEntity fromDomain(Transaction domain) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.id = domain.getId();
        entity.idempotencyKey = domain.getIdempotencyKey();
        entity.sourceAccountId = domain.getSourceAccountId();
        entity.destinationAccountId = domain.getDestinationAccountId();
        entity.amount = domain.getAmount();
        entity.currency = domain.getCurrency();
        entity.status = domain.getStatus();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}