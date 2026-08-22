// infrastructure/adapter/in/rest/dto/CreateTransactionRequest.java
package com.engine.financial_transaction_engine.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(
    UUID sourceAccountId,
    UUID destinationAccountId,
    BigDecimal amount,
    String currency
) {}