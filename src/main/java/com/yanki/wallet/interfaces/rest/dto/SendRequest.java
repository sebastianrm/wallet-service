package com.yanki.wallet.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para envío P2P.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Schema(description = "Datos para enviar dinero P2P")
public record SendRequest(

        @Schema(description = "Teléfono del destinatario", example = "+51999888777")
        @NotBlank(message = "El teléfono del destinatario es obligatorio")
        String recipientPhone,

        @Schema(description = "Monto a enviar", example = "100.00")
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal amount
) {
}
