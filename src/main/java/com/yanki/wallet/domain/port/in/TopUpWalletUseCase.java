package com.yanki.wallet.domain.port.in;
import com.yanki.wallet.domain.model.Transaction;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
/** Puerto de entrada para carga de saldo (top-up). */
public interface TopUpWalletUseCase {
    Single<Transaction> execute(String walletId, BigDecimal amount);
}
