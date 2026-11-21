package com.yanki.wallet.domain.port.out;
import com.yanki.wallet.domain.model.Wallet;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
/** Puerto de salida para la persistencia de wallets. */
public interface WalletPersistencePort {
    Maybe<Wallet> findById(String id);
    Maybe<Wallet> findByPhoneNumber(String phoneNumber);
    Single<Boolean> existsByCustomerId(String customerId);
    Single<Wallet> save(Wallet wallet);
}
