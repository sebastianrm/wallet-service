package com.yanki.wallet.domain.port.out;
import com.yanki.wallet.domain.event.WalletOpenedEvent;
import com.yanki.wallet.domain.event.WalletTransactionEvent;
import com.yanki.wallet.domain.event.WalletTopupRequestedEvent;
import io.reactivex.rxjava3.core.Completable;
/** Puerto de salida para publicar eventos de wallet hacia Apache Kafka. */
public interface EventPublisherPort {
    Completable publishWalletOpened(WalletOpenedEvent event);
    Completable publishTransactionRegistered(WalletTransactionEvent event);
    Completable publishTopupRequested(WalletTopupRequestedEvent event);
}
