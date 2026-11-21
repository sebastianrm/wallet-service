package com.yanki.wallet.domain.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
/** Evento deuda vencida. Topic: yanki.credit.overdue.v1 */
@Getter @Builder @AllArgsConstructor
public class CreditOverdueEvent {
    private final String customerId;
    private final Boolean hasOverdueDebt;
    private final LocalDateTime occurredAt;
}
