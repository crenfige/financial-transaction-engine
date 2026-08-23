package com.engine.financial_transaction_engine;

import com.engine.financial_transaction_engine.infrastructure.adapter.in.rest.dto.CreateTransactionRequest;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.OutboxEntity;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.OutboxJpaRepository;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.TransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class TransactionEngineE2ETest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionJpaRepository transactionRepo;

    @Autowired
    private OutboxJpaRepository outboxRepo;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void cleanDatabase() {
        outboxRepo.deleteAll();
        transactionRepo.deleteAll();
    }

    @Test
    void shouldProcessTransactionEndToEndWithOutboxAndKafka() {
        String idempotencyKey = "TX-E2E-" + UUID.randomUUID();
        CreateTransactionRequest request = new CreateTransactionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("750.00"),
                "USD"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);

        HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/transactions",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var transaction = transactionRepo.findByIdempotencyKey(idempotencyKey);
        assertThat(transaction).isPresent();
        assertThat(transaction.get().getAmount()).isEqualByComparingTo(new BigDecimal("750.00"));

        await().atMost(5, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            var outboxMessages = outboxRepo.findAll();
            assertThat(outboxMessages).isNotEmpty();
            assertThat(outboxMessages.get(0).getStatus()).isEqualTo(OutboxEntity.OutboxStatus.PUBLISHED);
        });

        await().atMost(5, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            String redisValue = redisTemplate.opsForValue().get("processed:transaction:" + idempotencyKey);
            assertThat(redisValue).isEqualTo("COMPLETED");
        });
    }

    @Test
    void shouldPublishToDltWhenSettlementFails() {
        String invalidPayload = "{\"foo\": \"bar\"}";

        kafkaTemplate.send("financial.transactions.v1", "KEY-FAIL", invalidPayload);

        await()
                .atMost(6, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    String redisValue = redisTemplate.opsForValue().get("processed:transaction:KEY-FAIL");
                    assertThat(redisValue).isNull();
                });
    }
}