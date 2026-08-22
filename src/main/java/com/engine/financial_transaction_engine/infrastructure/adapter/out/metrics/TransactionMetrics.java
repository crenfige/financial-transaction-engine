package com.engine.financial_transaction_engine.infrastructure.adapter.out.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionMetrics {

    private final Counter processedTransactionsCounter;
    private final Counter duplicateTransactionsCounter;
    private final Counter dltTransactionsCounter;
    private final MeterRegistry meterRegistry;

    public TransactionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.processedTransactionsCounter = Counter.builder("transactions.processed.total")
                .description("Total de transacciones procesadas exitosamente")
                .tag("status", "SUCCESS")
                .register(meterRegistry);

        this.duplicateTransactionsCounter = Counter.builder("transactions.duplicates.total")
                .description("Total de transacciones duplicadas descartadas por idempotencia")
                .tag("status", "DUPLICATE")
                .register(meterRegistry);

        this.dltTransactionsCounter = Counter.builder("transactions.dlt.total")
                .description("Total de eventos derivados a Dead Letter Topic")
                .tag("status", "FAILED")
                .register(meterRegistry);
    }

    public void incrementProcessed() {
        processedTransactionsCounter.increment();
    }

    public void incrementDuplicate() {
        duplicateTransactionsCounter.increment();
    }

    public void incrementDlt() {
        dltTransactionsCounter.increment();
    }

    public void recordAmount(BigDecimal amount, String currency) {
        Counter.builder("transactions.volume.amount")
                .description("Monto total procesado en transacciones")
                .tag("currency", currency)
                .register(meterRegistry)
                .increment(amount.doubleValue());
    }
}