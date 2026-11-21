package com.yanki.wallet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad pura de dominio que representa una transacción del wallet.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class Transaction {

    private final String id;
    private final String walletId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final TransactionStatus status;
    private final String counterpartyPhone;
    private final String counterpartyWalletId;
    private final String referenceId;
    private final String description;
    private final LocalDateTime occurredAt;
    private final LocalDateTime createdAt;

    public Transaction complete() {
        return this.toBuilder().status(TransactionStatus.COMPLETED).build();
    }

    public Transaction reject() {
        return this.toBuilder().status(TransactionStatus.REJECTED).build();
    }
}
