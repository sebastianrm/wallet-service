package com.yanki.wallet.domain.port.in;
import com.yanki.wallet.domain.model.Wallet;
import io.reactivex.rxjava3.core.Single;
/** Puerto de entrada para la apertura de wallets. */
public interface OpenWalletUseCase {
    Single<Wallet> execute(String customerId, String phoneNumber, String imei);
}
