package com.yanki.wallet.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para operaciones de monto (top-up, withdraw).
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Schema(description = "Datos de monto para top-up o retiro")
public record AmountRequest(

        @Schema(description = "Monto", example = "100.00")
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal amount
) {
}
