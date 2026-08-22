package com.engine.financial_transaction_engine.infrastructure.adapter.in.kafka;

import com.engine.financial_transaction_engine.infrastructure.adapter.out.metrics.TransactionMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class TransactionDltConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionDltConsumer.class);
    private final TransactionMetrics metrics;

    public TransactionDltConsumer(TransactionMetrics metrics) {
        this.metrics = metrics;
    }

    @KafkaListener(
            topics = "financial.transactions.v1.dlt",
            groupId = "transaction-dlt-consumer-group"
    )
    public void processDltMessage(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage
    ) {
        metrics.incrementDlt();
        log.error("CRITICAL: Poison pill or failed message received in DLT [{}]. Cause: {}. Payload: {}",
                topic, exceptionMessage, payload);
    }
}