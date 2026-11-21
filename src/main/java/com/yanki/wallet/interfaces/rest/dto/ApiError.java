package com.yanki.wallet.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de error estandarizado para las respuestas REST.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle de un error")
public class ApiError {

    @Schema(description = "Fecha y hora del error (ISO 8601).", example = "2024-01-15T12:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "Código de estado HTTP de la respuesta.", example = "400")
    private int status;

    @Schema(description = "Razón descriptiva del estado HTTP.", example = "Bad Request")
    private String error;

    @Schema(description = "Código de error de negocio para identificación programática.", example = "VALIDATION_ERROR")
    private String code;

    @Schema(description = "Mensaje descriptivo del error en lenguaje humano.", example = "Datos de entrada inválidos")
    private String message;

    @Schema(description = "Ruta del endpoint donde ocurrió el error.", example = "/api/wallets/123")
    private String path;

    @Schema(description = "Detalles adicionales del error, como campos inválidos y sus mensajes.", example = "{\"amount\":\"El monto es obligatorio\"}")
    private Map<String, Object> details;
}
