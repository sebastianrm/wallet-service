package com.yanki.wallet.infrastructure.persistence.mapper;

import com.yanki.wallet.domain.model.Wallet;
import com.yanki.wallet.domain.model.WalletStatus;
import com.yanki.wallet.infrastructure.persistence.document.WalletDocument;

/**
 * Traductor estático entre {@link Wallet} (dominio) y {@link WalletDocument} (persistencia).
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
public final class WalletDocumentMapper {

    private WalletDocumentMapper() {
        // Clase de utilidades
    }

    /**
     * Convierte un documento MongoDB a entidad de dominio.
     *
     * @param document documento de MongoDB
     * @return entidad de dominio
     */
    public static Wallet toDomain(WalletDocument document) {
        return Wallet.builder()
                .id(document.getId())
                .customerId(document.getCustomerId())
                .phoneNumber(document.getPhoneNumber())
                .imei(document.getImei())
                .balance(document.getBalance())
                .status(WalletStatus.valueOf(document.getStatus()))
                .associatedDebitCardId(document.getAssociatedDebitCardId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * Convierte una entidad de dominio a documento MongoDB.
     *
     * @param wallet entidad de dominio
     * @return documento de MongoDB
     */
    public static WalletDocument toDocument(Wallet wallet) {
        return WalletDocument.builder()
                .id(wallet.getId())
                .customerId(wallet.getCustomerId())
                .phoneNumber(wallet.getPhoneNumber())
                .imei(wallet.getImei())
                .balance(wallet.getBalance())
                .status(wallet.getStatus().name())
                .associatedDebitCardId(wallet.getAssociatedDebitCardId())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
