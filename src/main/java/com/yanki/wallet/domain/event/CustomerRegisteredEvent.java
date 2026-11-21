package com.yanki.wallet.domain.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
@Getter @Builder @AllArgsConstructor
public class CustomerRegisteredEvent {
    private final String customerId;
    private final String documentType;
    private final String documentNumber;
    private final String phoneNumber;
    private final String email;
    private final LocalDateTime occurredAt;
}
