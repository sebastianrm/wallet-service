package com.yanki.wallet.domain.port.in;
import com.yanki.wallet.domain.model.Transaction;
import com.yanki.wallet.domain.model.Wallet;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
/** Puerto de entrada para consulta de wallets y transacciones. */
public interface GetWalletUseCase {
    Maybe<Wallet> findById(String walletId);
    Maybe<Wallet> findByPhoneNumber(String phoneNumber);
    Flowable<Transaction> getTransactions(String walletId);
}
