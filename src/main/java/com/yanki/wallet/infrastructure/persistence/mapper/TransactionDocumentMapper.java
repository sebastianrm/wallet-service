package com.yanki.wallet.infrastructure.persistence.mapper;

import com.yanki.wallet.domain.model.Transaction;
import com.yanki.wallet.domain.model.TransactionStatus;
import com.yanki.wallet.domain.model.TransactionType;
import com.yanki.wallet.infrastructure.persistence.document.TransactionDocument;

/**
 * Traductor estático entre {@link Transaction} (dominio) y {@link TransactionDocument} (persistencia).
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
public final class TransactionDocumentMapper {

    private TransactionDocumentMapper() {
        // Clase de utilidades
    }

    /**
     * Convierte un documento MongoDB a entidad de dominio.
     *
     * @param document documento de MongoDB
     * @return entidad de dominio
     */
    public static Transaction toDomain(TransactionDocument document) {
        return Transaction.builder()
                .id(document.getId())
                .walletId(document.getWalletId())
                .type(TransactionType.valueOf(document.getType()))
                .amount(document.getAmount())
                .balanceAfter(document.getBalanceAfter())
                .status(TransactionStatus.valueOf(document.getStatus()))
                .counterpartyPhone(document.getCounterpartyPhone())
                .counterpartyWalletId(document.getCounterpartyWalletId())
                .referenceId(document.getReferenceId())
                .description(document.getDescription())
                .occurredAt(document.getOccurredAt())
                .createdAt(document.getCreatedAt())
                .build();
    }

    /**
     * Convierte una entidad de dominio a documento MongoDB.
     *
     * @param transaction entidad de dominio
     * @return documento de MongoDB
     */
    public static TransactionDocument toDocument(Transaction transaction) {
        return TransactionDocument.builder()
                .id(transaction.getId())
                .walletId(transaction.getWalletId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .status(transaction.getStatus().name())
                .counterpartyPhone(transaction.getCounterpartyPhone())
                .counterpartyWalletId(transaction.getCounterpartyWalletId())
                .referenceId(transaction.getReferenceId())
                .description(transaction.getDescription())
                .occurredAt(transaction.getOccurredAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
