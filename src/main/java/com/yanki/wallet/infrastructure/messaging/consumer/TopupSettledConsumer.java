package com.yanki.wallet.infrastructure.messaging.consumer;

import com.yanki.wallet.domain.event.WalletTopupSettledEvent;
import com.yanki.wallet.domain.event.Topics;
import com.yanki.wallet.domain.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka que procesa la respuesta de top-up liquidado.
 *
 * <p>Topic: yanki.wallet.topup.settled.v1</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@Component
public class TopupSettledConsumer {

    private final WalletService walletService;

    public TopupSettledConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @KafkaListener(topics = Topics.WALLET_TOPUP_SETTLED, groupId = "${spring.kafka.consumer.group-id:wallet-service}",
            containerFactory = "walletTopupSettledKafkaListenerContainerFactory")
    public void consume(WalletTopupSettledEvent event) {
        log.info("[Kafka-Consumer] Recepcionado evento topup.settled.v1: topupId={} walletId={} status={} amount={}",
                event.getTopupId(), event.getWalletId(), event.getStatus(), event.getAmount());
        log.debug("[Kafka-Consumer] Detalles del evento topup.settled.v1: occurredAt={}", event.getOccurredAt());

        try {
            walletService.processTopupSettled(event).subscribe(
                    () -> log.info("[Kafka-Consumer] Top-up settled procesado exitosamente: topupId={} walletId={}",
                            event.getTopupId(), event.getWalletId()),
                    error -> {
                        log.error("[Kafka-Consumer] Error procesando top-up settled: topupId={} walletId={} - {}",
                                event.getTopupId(), event.getWalletId(), error.getMessage(), error);
                        log.error("[Kafka-Consumer] Causa raíz del error: {}", error.getCause());
                    }
            );
        } catch (Exception e) {
            log.error("[Kafka-Consumer] Excepción al suscribirse al procesamiento de top-up settled: topupId={} - {}",
                    event.getTopupId(), e.getMessage(), e);
        }
    }
}
