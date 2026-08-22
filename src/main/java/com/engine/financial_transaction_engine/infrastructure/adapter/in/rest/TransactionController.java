package com.engine.financial_transaction_engine.infrastructure.adapter.in.rest;

import com.engine.financial_transaction_engine.application.service.ProcessTransactionService;
import com.engine.financial_transaction_engine.infrastructure.adapter.in.rest.dto.CreateTransactionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final ProcessTransactionService transactionService;

    public TransactionController(ProcessTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateTransactionRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        UUID transactionId = transactionService.processPayment(
                idempotencyKey,
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                request.currency()
        );

        return ResponseEntity
                .created(URI.create("/api/v1/transactions/" + transactionId))
                .body(Map.of(
                        "transactionId", transactionId,
                        "idempotencyKey", idempotencyKey,
                        "status", "PENDING"
                ));
    }
}