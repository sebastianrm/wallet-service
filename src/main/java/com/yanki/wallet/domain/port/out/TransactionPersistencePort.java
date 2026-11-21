package com.yanki.wallet.domain.port.out;
import com.yanki.wallet.domain.model.Transaction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
/** Puerto de salida para la persistencia de transacciones. */
public interface TransactionPersistencePort {
    Single<Transaction> save(Transaction transaction);
    Flowable<Transaction> findByWalletId(String walletId);
    Single<Transaction> findById(String id);
    Completable updateStatus(Transaction transaction);
}
