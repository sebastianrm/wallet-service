package com.yanki.wallet.domain.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
@Getter @Builder @AllArgsConstructor
public class CardOpenedEvent {
    private final String cardId;
    private final String customerId;
    private final String cardType;
    private final LocalDateTime occurredAt;
}
