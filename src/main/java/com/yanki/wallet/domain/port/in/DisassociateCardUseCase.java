package com.yanki.wallet.domain.port.in;
import com.yanki.wallet.domain.model.Wallet;
import io.reactivex.rxjava3.core.Single;
/** Puerto de entrada para desasociación de tarjeta débito. */
public interface DisassociateCardUseCase {
    Single<Wallet> execute(String walletId);
}
