package com.engine.financial_transaction_engine.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Transaction {
    private final UUID id;
    private final String idempotencyKey;
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final BigDecimal amount;
    private final String currency;
    private TransactionStatus status;
    private final Instant createdAt;

    public Transaction(UUID id, String idempotencyKey, UUID sourceAccountId, 
                       UUID destinationAccountId, BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId, "Source account cannot be null");
        this.destinationAccountId = Objects.requireNonNull(destinationAccountId, "Destination account cannot be null");
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.status = TransactionStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void markAsCompleted() { this.status = TransactionStatus.COMPLETED; }
    public void markAsFailed() { this.status = TransactionStatus.FAILED; }
}