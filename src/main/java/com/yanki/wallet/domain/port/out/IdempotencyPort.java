package com.yanki.wallet.domain.port.out;
import io.reactivex.rxjava3.core.Single;
/** Puerto de salida para verificar la idempotencia de eventos Kafka. */
public interface IdempotencyPort {
    Single<Boolean> checkAndMarkProcessed(String idempotencyKey);
}
