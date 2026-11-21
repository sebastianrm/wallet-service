package com.yanki.wallet.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para la solicitud de apertura de wallet.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Schema(description = "Datos para abrir una nueva wallet")
public record OpenWalletRequest(

        @Schema(description = "Tipo de documento", example = "DNI")
        @NotBlank(message = "El tipo de documento es obligatorio")
        String documentType,

        @Schema(description = "Número de documento", example = "70100001")
        @NotBlank(message = "El número de documento es obligatorio")
        String documentNumber,

        @Schema(description = "Número de teléfono", example = "+51999888777")
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Formato de teléfono inválido")
        String phone,

        @Schema(description = "IMEI del celular", example = "353456789012345")
        @NotBlank(message = "El IMEI es obligatorio")
        @Pattern(regexp = "^[0-9]{15}$", message = "El IMEI debe tener 15 dígitos")
        String imei,

        @Schema(description = "Correo electrónico", example = "usuario@email.com")
        @NotBlank(message = "El correo es obligatorio")
        String email,

        @Schema(description = "Nombres", example = "Juan")
        String firstName,

        @Schema(description = "Apellidos", example = "Pérez")
        String lastName,

        @Schema(description = "Fecha de nacimiento", example = "1990-01-01")
        String birthDate
) {
}
