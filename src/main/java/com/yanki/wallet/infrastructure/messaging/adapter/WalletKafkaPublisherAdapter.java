package com.yanki.wallet.infrastructure.messaging.adapter;

import com.yanki.wallet.domain.event.Topics;
import com.yanki.wallet.domain.event.WalletOpenedEvent;
import com.yanki.wallet.domain.event.WalletTransactionEvent;
import com.yanki.wallet.domain.event.WalletTopupRequestedEvent;
import com.yanki.wallet.domain.port.out.EventPublisherPort;
import io.reactivex.rxjava3.core.Completable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adaptador de salida que publica eventos de wallet hacia Apache Kafka.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@Component
public class WalletKafkaPublisherAdapter implements EventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WalletKafkaPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Completable publishWalletOpened(WalletOpenedEvent event) {
        log.info("[Kafka] Publicando wallet.opened.v1: walletId={} customerId={}", event.getWalletId(), event.getCustomerId());
        log.debug("[Kafka] Clase del evento publicado: {}", event.getClass().getName());
        log.debug("[Kafka] WalletOpenedEvent campos -> walletId={}, customerId={}, occurredAt={}",
                event.getWalletId(), event.getCustomerId(), event.getOccurredAt());
        return Completable.fromPublisher(
                Mono.fromFuture(() -> kafkaTemplate.send(Topics.WALLET_OPENED, event.getWalletId(), event)).then()
        )
                .doOnComplete(() -> log.debug("[Kafka] Evento wallet.opened.v1 publicado: walletId={}", event.getWalletId()))
                .doOnError(error -> log.error("[Kafka] Error publicando wallet.opened.v1: walletId={} - {}",
                        event.getWalletId(), error.getMessage(), error));
    }

    @Override
    public Completable publishTransactionRegistered(WalletTransactionEvent event) {
        log.info("[Kafka] Publicando wallet.transaction.registered.v1: txId={} walletId={} type={}",
                event.getTransactionId(), event.getWalletId(), event.getType());
        log.debug("[Kafka] Clase del evento publicado: {}", event.getClass().getName());
        log.debug("[Kafka] WalletTransactionEvent campos -> transactionId={}, walletId={}, type={}, amount={}, balanceAfter={}, status={}, occurredAt={}",
                event.getTransactionId(), event.getWalletId(), event.getType(), event.getAmount(), event.getBalanceAfter(), event.getStatus(), event.getOccurredAt());
        return Completable.fromPublisher(
                Mono.fromFuture(() -> kafkaTemplate.send(Topics.WALLET_TRANSACTION_REGISTERED, event.getTransactionId(), event)).then()
        )
                .doOnComplete(() -> log.debug("[Kafka] Evento wallet.transaction.registered.v1 publicado: txId={}", event.getTransactionId()))
                .doOnError(error -> log.error("[Kafka] Error publicando wallet.transaction.registered.v1: txId={} - {}",
                        event.getTransactionId(), error.getMessage(), error));
    }

    @Override
    public Completable publishTopupRequested(WalletTopupRequestedEvent event) {
        log.info("[Kafka] Publicando wallet.topup.requested.v1: topupId={} walletId={} amount={}",
                event.getTopupId(), event.getWalletId(), event.getAmount());
        log.debug("[Kafka] Clase del evento publicado: {}", event.getClass().getName());
        log.debug("[Kafka] WalletTopupRequestedEvent campos -> topupId={}, walletId={}, customerId={}, amount={}, occurredAt={}",
                event.getTopupId(), event.getWalletId(), event.getCustomerId(), event.getAmount(), event.getOccurredAt());
        return Completable.fromPublisher(
                Mono.fromFuture(() -> kafkaTemplate.send(Topics.WALLET_TOPUP_REQUESTED, event.getTopupId(), event)).then()
        )
                .doOnComplete(() -> log.debug("[Kafka] Evento wallet.topup.requested.v1 publicado: topupId={}", event.getTopupId()))
                .doOnError(error -> log.error("[Kafka] Error publicando wallet.topup.requested.v1: topupId={} - {}",
                        event.getTopupId(), error.getMessage(), error));
    }
}
