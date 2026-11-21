package com.yanki.wallet.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Evento publicado cuando se registra una transacción. Topic: yanki.wallet.transaction.registered.v1 */
@Getter @Builder @AllArgsConstructor
public class WalletTransactionEvent {
    private final String transactionId;
    private final String walletId;
    private final String type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String status;
    private final String counterpartyPhone;
    private final String counterpartyWalletId;
    private final String referenceId;
    private final LocalDateTime occurredAt;
}
