/*package com.engine.financial_transaction_engine;

import com.engine.financial_transaction_engine.infrastructure.adapter.in.rest.dto.CreateTransactionRequest;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.OutboxEntity;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.OutboxJpaRepository;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.TransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TransactionEngineIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0").asCompatibleSubstituteFor("apache/kafka"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionJpaRepository transactionRepo;

    @Autowired
    private OutboxJpaRepository outboxRepo;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanDatabase() {
        outboxRepo.deleteAll();
        transactionRepo.deleteAll();
    }

    @Test
    void shouldProcessTransactionEndToEndWithOutboxAndKafka() {
        String idempotencyKey = "TX-IT-" + UUID.randomUUID();
        CreateTransactionRequest request = new CreateTransactionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("350.00"),
                "USD"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);

        HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);

        // 1. Invocar el endpoint POST
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/transactions",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Verificar que se persistió la transacción
        var transaction = transactionRepo.findByIdempotencyKey(idempotencyKey);
        assertThat(transaction).isPresent();
        assertThat(transaction.get().getAmount()).isEqualByComparingTo(new BigDecimal("350.00"));

        // 3. Esperar a que el OutboxPublisher procese y publique el evento en Kafka
        await().atMost(5, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            var outboxMessages = outboxRepo.findAll();
            assertThat(outboxMessages).isNotEmpty();
            assertThat(outboxMessages.get(0).getStatus()).isEqualTo(OutboxEntity.OutboxStatus.PUBLISHED);
        });

        // 4. Esperar a que el Consumer de Kafka lo procese y lo registre en Redis
        await().atMost(5, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            String redisValue = redisTemplate.opsForValue().get("processed:transaction:" + idempotencyKey);
            assertThat(redisValue).isEqualTo("COMPLETED");
        });
    }
}*/