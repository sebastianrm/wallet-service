package com.yanki.wallet.domain.port.in;
import com.yanki.wallet.domain.model.Wallet;
import io.reactivex.rxjava3.core.Single;
/** Puerto de entrada para asociación de tarjeta débito. */
public interface AssociateCardUseCase {
    Single<Wallet> execute(String walletId, String cardId);
}
