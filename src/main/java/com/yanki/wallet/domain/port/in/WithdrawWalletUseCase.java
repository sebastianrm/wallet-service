package com.yanki.wallet.domain.port.in;
import com.yanki.wallet.domain.model.Transaction;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
/** Puerto de entrada para retiro de saldo a cuenta bancaria. */
public interface WithdrawWalletUseCase {
    Single<Transaction> withdraw(String walletId, BigDecimal amount);
}
