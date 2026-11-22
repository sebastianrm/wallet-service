package com.yanki.wallet.interfaces.rest;

import com.mongodb.MongoWriteException;
import com.yanki.wallet.domain.exception.BusinessException;
import com.yanki.wallet.domain.exception.ErrorCodes;
import com.yanki.wallet.interfaces.rest.dto.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para wallet-service.
 *
 * <p>Intercepta excepciones lanzadas por los controladores REST y las mapea a
 * respuestas HTTP estandarizadas siguiendo el formato {@link ApiError}.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex, ServerWebExchange exchange) {
        HttpStatus status = mapErrorCodeToStatus(ex.getCode());
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.warn("[BusinessException] {} {} - Error de negocio: code={} message={} walletId={}",
                method, path, ex.getCode(), ex.getMessage(), extractWalletId(path));
        log.debug("[BusinessException] Stack trace para code={}:", ex.getCode(), ex);

        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(ex.getCode())
                .message(ex.getMessage())
                .path(path)
                .build();

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleValidationException(WebExchangeBindException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        Map<String, Object> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
            log.warn("[ValidationError] {} {} - Campo: {} error: {}", method, path, fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("[WebExchangeBindException] {} {} - {} error(es) de validación: {}", method, path, details.size(), details);

        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(ErrorCodes.VALIDATION_ERROR)
                .message("Datos de entrada inválidos")
                .path(path)
                .details(details)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MongoWriteException.class)
    public ResponseEntity<ApiError> handleMongoWriteException(MongoWriteException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // E11000 = duplicate key error
        com.mongodb.WriteError writeError = ex.getError();
        if (writeError != null && writeError.getCode() == 11000) {
            log.warn("[MongoWriteException] {} {} - Clave duplicada en MongoDB: code=11000 message={}",
                    method, path, ex.getMessage());

            ApiError error = ApiError.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.CONFLICT.value())
                    .error(HttpStatus.CONFLICT.getReasonPhrase())
                    .code(ErrorCodes.WALLET_ALREADY_EXISTS)
                    .message("Ya existe una wallet registrada con este documento, número de celular o IMEI. Verifique los datos e intente con información diferente.")
                    .path(path)
                    .build();

            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // Other MongoDB write errors
        log.error("[MongoWriteException] {} {} - Error de escritura en MongoDB: code={} message={}",
                method, path, writeError != null ? writeError.getCode() : "N/A", ex.getMessage(), ex);

        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .code("INTERNAL_ERROR")
                .message("Error interno del servidor")
                .path(path)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.error("[UnhandledException] {} {} - Error interno no controlado: class={} message={}",
                method, path, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        log.error("[UnhandledException] Causa raíz: {}", ex.getCause());

        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .code("INTERNAL_ERROR")
                .message("Error interno del servidor")
                .path(path)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Extrae el walletId de la ruta para facilitar el seguimiento en logs.
     */
    private String extractWalletId(String path) {
        try {
            // Ejemplo: /api/wallets/WALLET-123/topup -> WALLET-123
            String[] parts = path.split("/");
            if (parts.length >= 4) {
                return parts[3];
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer walletId de la ruta: {}", path);
        }
        return "N/A";
    }

    private HttpStatus mapErrorCodeToStatus(String code) {
        return switch (code) {
            case ErrorCodes.WALLET_NOT_FOUND,
                 ErrorCodes.CUSTOMER_NOT_FOUND,
                 ErrorCodes.RECIPIENT_NOT_FOUND,
                 ErrorCodes.DEBIT_CARD_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCodes.WALLET_ALREADY_EXISTS,
                 ErrorCodes.WALLET_INACTIVE,
                 ErrorCodes.WALLET_BLOCKED_RISK,
                 ErrorCodes.CUSTOMER_HAS_OVERDUE_DEBT,
                 ErrorCodes.INSUFFICIENT_BALANCE,
                 ErrorCodes.INVALID_AMOUNT,
                 ErrorCodes.CANNOT_SEND_TO_SELF,
                 ErrorCodes.P2P_DAILY_LIMIT_EXCEEDED,
                 ErrorCodes.DEBIT_CARD_REQUIRED,
                 ErrorCodes.CARD_ALREADY_ASSOCIATED,
                 ErrorCodes.NO_CARD_ASSOCIATED,
                 ErrorCodes.BANK_ACCOUNT_REQUIRED,
                 ErrorCodes.WALLET_LIMIT_EXCEEDED,
                 ErrorCodes.INSUFFICIENT_FUNDS,
                 ErrorCodes.INVALID_WITHDRAWAL_DESTINATION,
                 ErrorCodes.INVALID_DEBIT_CARD -> HttpStatus.CONFLICT;
            case ErrorCodes.VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
