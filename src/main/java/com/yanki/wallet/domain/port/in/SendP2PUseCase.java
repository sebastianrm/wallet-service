package com.yanki.wallet.domain.port.in;
import com.yanki.wallet.domain.model.Transaction;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
/** Puerto de entrada para envío P2P entre wallets. */
public interface SendP2PUseCase {
    Single<Transaction> execute(String walletId, String recipientPhone, BigDecimal amount);
}
