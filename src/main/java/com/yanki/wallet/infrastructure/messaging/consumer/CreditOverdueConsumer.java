package com.yanki.wallet.infrastructure.messaging.consumer;

import com.yanki.wallet.domain.event.CreditOverdueEvent;
import com.yanki.wallet.domain.event.Topics;
import com.yanki.wallet.domain.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka que procesa eventos de deuda vencida.
 *
 * <p>Topic: yanki.credit.overdue.v1 (de credits-service)</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@Component
public class CreditOverdueConsumer {

    private final WalletService walletService;

    public CreditOverdueConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @KafkaListener(topics = Topics.CREDIT_OVERDUE, groupId = "${spring.kafka.consumer.group-id:wallet-service}",
            containerFactory = "creditOverdueKafkaListenerContainerFactory")
    public void consume(CreditOverdueEvent event) {
        log.info("[Kafka-Consumer] Recepcionado evento credit.overdue.v1: customerId={} hasOverdueDebt={}",
                event.getCustomerId(), event.getHasOverdueDebt());
        log.debug("[Kafka-Consumer] Detalles del evento credit.overdue.v1: occurredAt={}", event.getOccurredAt());

        try {
            walletService.processCreditOverdue(event.getCustomerId(), event.getHasOverdueDebt()).subscribe(
                    () -> log.info("[Kafka-Consumer] Credit overdue procesado exitosamente: customerId={}",
                            event.getCustomerId()),
                    error -> {
                        log.error("[Kafka-Consumer] Error procesando credit overdue: customerId={} - {}",
                                event.getCustomerId(), error.getMessage(), error);
                        log.error("[Kafka-Consumer] Causa raíz del error: {}", error.getCause());
                    }
            );
        } catch (Exception e) {
            log.error("[Kafka-Consumer] Excepción al suscribirse al procesamiento de credit overdue: customerId={} - {}",
                    event.getCustomerId(), e.getMessage(), e);
        }
    }
}
