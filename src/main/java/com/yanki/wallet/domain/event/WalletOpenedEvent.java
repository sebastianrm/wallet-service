package com.yanki.wallet.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

/** Evento publicado cuando se abre un nuevo wallet. Topic: yanki.wallet.opened.v1 */
@Getter @Builder @AllArgsConstructor
public class WalletOpenedEvent {
    private final String walletId;
    private final String customerId;
    private final String phoneNumber;
    private final LocalDateTime occurredAt;
}
