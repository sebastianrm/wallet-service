package com.yanki.wallet.domain.port.out;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
/** Puerto de salida para caché Redis. */
public interface CachePort {
    Maybe<BigDecimal> getBalance(String walletId);
    Completable setBalance(String walletId, BigDecimal balance);
    Completable invalidateBalance(String walletId);
    Maybe<String> getWalletIdByPhone(String phoneNumber);
    Single<Void> setWalletIdByPhone(String phoneNumber, String walletId);
}
