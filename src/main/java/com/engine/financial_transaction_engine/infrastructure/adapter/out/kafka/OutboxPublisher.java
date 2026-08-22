package com.engine.financial_transaction_engine.infrastructure.adapter.out.kafka;

import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.OutboxEntity;
import com.engine.financial_transaction_engine.infrastructure.adapter.out.persistence.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String TOPIC = "financial.transactions.v1";

    private final OutboxJpaRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxJpaRepository outboxRepo, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepo = outboxRepo;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEntity> pendingEvents = outboxRepo.findTop50ByStatusOrderByCreatedAtAsc(
                OutboxEntity.OutboxStatus.PENDING
        );

        for (OutboxEntity event : pendingEvents) {
            try {
                kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.markPublished();
                                outboxRepo.save(event);
                                log.info("Event {} published to topic {}", event.getId(), TOPIC);
                            } else {
                                log.error("Failed to publish outbox event: {}", event.getId(), ex);
                            }
                        });
            } catch (Exception ex) {
                log.error("Error submitting event to Kafka broker: {}", event.getId(), ex);
            }
        }
    }
}