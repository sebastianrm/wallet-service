package com.yanki.wallet.domain.event;

/** Constantes de los topics de Kafka utilizados por wallet-service. */
public final class Topics {
    private Topics() {}
    public static final String WALLET_OPENED = "yanki.wallet.opened.v1";
    public static final String WALLET_TRANSACTION_REGISTERED = "yanki.wallet.transaction.registered.v1";
    public static final String WALLET_TOPUP_REQUESTED = "yanki.wallet.topup.requested.v1";
    public static final String WALLET_TOPUP_SETTLED = "yanki.wallet.topup.settled.v1";
    public static final String CUSTOMER_REGISTERED = "yanki.customer.registered.v1";
    public static final String CUSTOMER_UPDATED = "yanki.customer.updated.v1";
    public static final String CUSTOMER_DELETED = "yanki.customer.deleted.v1";
    public static final String CREDIT_OVERDUE = "yanki.credit.overdue.v1";
    public static final String CARD_OPENED = "yanki.card.opened.v1";
    public static final String CARD_CLOSED = "yanki.card.closed.v1";
}
