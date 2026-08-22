package com.engine.financial_transaction_engine.infrastructure.adapter.in.kafka;

import com.engine.financial_transaction_engine.infrastructure.adapter.out.metrics.TransactionMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Component
public class TransactionCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TransactionCreatedEventHandler.class);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionMetrics metrics;

    public TransactionCreatedEventHandler(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            TransactionMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @KafkaListener(
            topics = "financial.transactions.v1",
            groupId = "transaction-settlement-group",
            concurrency = "3"
    )
    public void handleTransactionEvent(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumer received event from partition {} at offset {}", partition, offset);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String transactionId = root.get("id").asText();
            String idempotencyKey = root.get("idempotencyKey").asText();
            BigDecimal amount = new BigDecimal(root.get("amount").asText());
            String currency = root.get("currency").asText();

            String redisKey = "processed:transaction:" + idempotencyKey;

            // Deduplicación atómica vía Redis SETNX
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, "COMPLETED", IDEMPOTENCY_TTL);

            if (Boolean.FALSE.equals(isNew)) {
                log.warn("Duplicate transaction event detected for key: {}. Skipping settlement.", idempotencyKey);
                metrics.incrementDuplicate();
                return;
            }

            // Registro de métricas de éxito y volumen
            metrics.incrementProcessed();
            metrics.recordAmount(amount, currency);

            log.info("Successfully settled transaction ID: {} with idempotency key: {}", transactionId, idempotencyKey);

        } catch (Exception e) {
            log.error("Error processing transaction event: {}", e.getMessage(), e);
            throw new RuntimeException("Triggering Kafka re-delivery", e);
        }
    }
}