package com.yanki.wallet.domain.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Evento top-up solicitado. Topic: yanki.wallet.topup.requested.v1 */
@Getter @Builder @AllArgsConstructor
public class WalletTopupRequestedEvent {
    private final String topupId;
    private final String walletId;
    private final String customerId;
    private final String debitCardId;
    private final BigDecimal amount;
    private final LocalDateTime occurredAt;
}
