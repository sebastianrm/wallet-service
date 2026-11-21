package com.yanki.wallet.domain.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Evento top-up liquidado. Topic: yanki.wallet.topup.settled.v1 */
@Getter @Builder @AllArgsConstructor
public class WalletTopupSettledEvent {
    private final String topupId;
    private final String walletId;
    private final String status;
    private final BigDecimal amount;
    private final String reason;
    private final LocalDateTime occurredAt;
}
