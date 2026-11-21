package com.yanki.wallet.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para asociación de tarjeta débito.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Schema(description = "Datos para asociar tarjeta débito")
public record AssociateCardRequest(

        @Schema(description = "Identificador de la tarjeta", example = "CARD-1234567890ABCDEF")
        @NotBlank(message = "El identificador de la tarjeta es obligatorio")
        String cardId
) {
}
