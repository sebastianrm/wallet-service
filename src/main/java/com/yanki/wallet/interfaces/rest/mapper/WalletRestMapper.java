package com.yanki.wallet.interfaces.rest.mapper;

import com.yanki.wallet.domain.model.Transaction;
import com.yanki.wallet.domain.model.Wallet;
import com.yanki.wallet.interfaces.rest.dto.TransactionResponse;
import com.yanki.wallet.interfaces.rest.dto.WalletResponse;

/**
 * Traductor estático entre modelos de dominio y DTOs REST.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
public final class WalletRestMapper {

    private WalletRestMapper() {
        // Clase de utilidades
    }

    /**
     * Convierte una wallet de dominio a DTO de respuesta.
     *
     * @param wallet entidad de dominio
     * @return DTO de respuesta
     */
    public static WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getCustomerId(),
                wallet.getPhoneNumber(),
                wallet.getBalance(),
                wallet.getStatus().name(),
                wallet.getAssociatedDebitCardId(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    /**
     * Convierte una transacción de dominio a DTO de respuesta.
     *
     * @param transaction entidad de dominio
     * @return DTO de respuesta
     */
    public static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getWalletId(),
                transaction.getType().name(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getStatus().name(),
                transaction.getCounterpartyPhone(),
                transaction.getReferenceId(),
                transaction.getDescription(),
                transaction.getOccurredAt()
        );
    }
}
