package com.yanki.wallet.domain.exception;

/**
 * Excepción de negocio de la plataforma Yanki (wallet-service).
 *
 * <p>Todos los errores de validación de reglas de negocio deben lanzar esta
 * excepción con un código específico definido en {@link ErrorCodes}.
 * El {@link com.yanki.wallet.interfaces.rest.GlobalExceptionHandler} mapea
 * cada código a un HTTP status adecuado.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
