package com.yanki.wallet.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para detalle de wallet.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Schema(description = "Detalle de una wallet")
public record WalletResponse(

        @Schema(description = "Identificador único de la wallet.", example = "WALLET-1234567890ABCDEF")
        String id,

        @Schema(description = "Identificador del cliente propietario (formato: documentType-documentNumber).", example = "DNI-70100001")
        String customerId,

        @Schema(description = "Número de teléfono asociado a la wallet.", example = "+51999888777")
        String phoneNumber,

        @Schema(description = "Saldo actual disponible en la wallet.", example = "1000.50")
        BigDecimal balance,

        @Schema(description = "Estado actual de la wallet.", example = "ACTIVE", allowableValues = {"ACTIVE", "BLOCKED", "CLOSED"})
        String status,

        @Schema(description = "Identificador de la tarjeta débito asociada para top-up (puede ser nulo).", example = "CARD-1234567890ABCDEF")
        String associatedDebitCardId,

        @Schema(description = "Fecha y hora de creación de la wallet (ISO 8601).", example = "2024-01-01T00:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Fecha y hora de última actualización de la wallet (ISO 8601).", example = "2024-01-15T12:00:00")
        LocalDateTime updatedAt
) {
}
