package com.yanki.wallet.domain.model;

import com.yanki.wallet.domain.exception.BusinessException;
import com.yanki.wallet.domain.exception.ErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entidad pura de dominio que representa un monedero móvil Yanki.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class Wallet {

    private final String id;
    private final String customerId;
    private final String phoneNumber;
    private final String imei;
    private final BigDecimal balance;
    private final WalletStatus status;
    private final String associatedDebitCardId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public boolean isActive() {
        return WalletStatus.ACTIVE == this.status;
    }

    public Wallet topUp(BigDecimal amount) {
        validateActive();
        validatePositiveAmount(amount);
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        return this.toBuilder()
                .balance(this.balance.add(normalized).setScale(2, RoundingMode.HALF_UP))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Wallet debit(BigDecimal amount) {
        validateActive();
        validatePositiveAmount(amount);
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = this.balance.subtract(normalized);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCodes.INSUFFICIENT_BALANCE,
                    "Saldo insuficiente: " + this.balance + " < " + normalized);
        }
        return this.toBuilder()
                .balance(newBalance.setScale(2, RoundingMode.HALF_UP))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Wallet associateCard(String cardId) {
        validateActive();
        if (cardId == null || cardId.isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "El identificador de la tarjeta no puede estar vacío");
        }
        return this.toBuilder().associatedDebitCardId(cardId).updatedAt(LocalDateTime.now()).build();
    }

    public Wallet disassociateCard() {
        validateActive();
        return this.toBuilder().associatedDebitCardId(null).updatedAt(LocalDateTime.now()).build();
    }

    public Wallet block() {
        return this.toBuilder().status(WalletStatus.BLOCKED).updatedAt(LocalDateTime.now()).build();
    }

    public Wallet unblock() {
        return this.toBuilder().status(WalletStatus.ACTIVE).updatedAt(LocalDateTime.now()).build();
    }

    public Wallet close() {
        if (this.balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "No se puede cerrar wallet con saldo distinto de 0");
        }
        return this.toBuilder().status(WalletStatus.CLOSED).updatedAt(LocalDateTime.now()).build();
    }

    private void validateActive() {
        if (this.status != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.WALLET_INACTIVE,
                    "La wallet no se encuentra activa para realizar operaciones");
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCodes.INVALID_AMOUNT, "El monto debe ser mayor a cero");
        }
    }
}
