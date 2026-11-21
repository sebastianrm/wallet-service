package com.yanki.wallet.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para transacción.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Schema(description = "Detalle de una transacción")
public record TransactionResponse(

        @Schema(description = "Identificador único de la transacción.", example = "TX-1234567890ABCDEF")
        String id,

        @Schema(description = "Identificador de la wallet a la que pertenece la transacción.", example = "WALLET-1234567890ABCDEF")
        String walletId,

        @Schema(description = "Tipo de transacción.", example = "SEND", allowableValues = {"TOPUP", "SEND", "RECEIVE", "WITHDRAWAL"})
        String type,

        @Schema(description = "Monto de la transacción.", example = "100.00")
        BigDecimal amount,

        @Schema(description = "Saldo de la wallet después de la transacción.", example = "900.50")
        BigDecimal balanceAfter,

        @Schema(description = "Estado actual de la transacción.", example = "COMPLETED", allowableValues = {"PENDING", "COMPLETED", "REJECTED"})
        String status,

        @Schema(description = "Número de teléfono de la contraparte (en operaciones P2P).", example = "+51999888777")
        String counterpartyPhone,

        @Schema(description = "Identificador de referencia externa de la transacción.", example = "REF-ABCDEF123456")
        String referenceId,

        @Schema(description = "Descripción humana de la transacción.", example = "Envío P2P")
        String description,

        @Schema(description = "Fecha y hora en que ocurrió la transacción (ISO 8601).", example = "2024-01-15T12:00:00")
        LocalDateTime occurredAt
) {
}
