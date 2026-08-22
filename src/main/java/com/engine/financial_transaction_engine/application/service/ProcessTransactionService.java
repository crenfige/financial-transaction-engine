package com.engine.financial_transaction_engine.application.service;

import com.engine.financial_transaction_engine.domain.model.Transaction;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.OutboxEntity;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.OutboxJpaRepository;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.TransactionJpaEntity;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.TransactionJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProcessTransactionService {

    private final TransactionJpaRepository transactionRepo;
    private final OutboxJpaRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public ProcessTransactionService(TransactionJpaRepository transactionRepo,
                                     OutboxJpaRepository outboxRepo,
                                     ObjectMapper objectMapper) {
        this.transactionRepo = transactionRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID processPayment(String idempotencyKey, UUID sourceId, UUID destId, BigDecimal amount, String currency) {
        var existing = transactionRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(transactionId, idempotencyKey, sourceId, destId, amount, currency);

        TransactionJpaEntity entity = TransactionJpaEntity.fromDomain(transaction);
        transactionRepo.save(entity);

        try {
            String payload = objectMapper.writeValueAsString(entity);
            OutboxEntity outboxMessage = new OutboxEntity(
                    UUID.randomUUID(),
                    "TRANSACTION",
                    transactionId.toString(),
                    "TransactionInitiated",
                    payload
            );
            outboxRepo.save(outboxMessage);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing outbox payload", e);
        }

        return transactionId;
    }
}