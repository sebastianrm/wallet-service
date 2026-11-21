package com.yanki.wallet.domain.exception;

/**
 * Códigos de error de negocio del wallet-service.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
public final class ErrorCodes {

    private ErrorCodes() {
        // Clase de constantes
    }

    public static final String WALLET_NOT_FOUND = "WALLET_NOT_FOUND";
    public static final String WALLET_ALREADY_EXISTS = "WALLET_ALREADY_EXISTS";
    public static final String WALLET_INACTIVE = "WALLET_INACTIVE";
    public static final String WALLET_BLOCKED_RISK = "WALLET_BLOCKED_RISK";
    public static final String CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND";
    public static final String CUSTOMER_HAS_OVERDUE_DEBT = "CUSTOMER_HAS_OVERDUE_DEBT";
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String INVALID_AMOUNT = "INVALID_AMOUNT";
    public static final String RECIPIENT_NOT_FOUND = "RECIPIENT_NOT_FOUND";
    public static final String CANNOT_SEND_TO_SELF = "CANNOT_SEND_TO_SELF";
    public static final String P2P_DAILY_LIMIT_EXCEEDED = "P2P_DAILY_LIMIT_EXCEEDED";
    public static final String DEBIT_CARD_REQUIRED = "DEBIT_CARD_REQUIRED";
    public static final String DEBIT_CARD_NOT_FOUND = "DEBIT_CARD_NOT_FOUND";
    public static final String CARD_ALREADY_ASSOCIATED = "CARD_ALREADY_ASSOCIATED";
    public static final String NO_CARD_ASSOCIATED = "NO_CARD_ASSOCIATED";
    public static final String BANK_ACCOUNT_REQUIRED = "BANK_ACCOUNT_REQUIRED";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String WALLET_LIMIT_EXCEEDED = "WALLET_LIMIT_EXCEEDED";
    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String INVALID_WITHDRAWAL_DESTINATION = "INVALID_WITHDRAWAL_DESTINATION";
    public static final String INVALID_DEBIT_CARD = "INVALID_DEBIT_CARD";
}
