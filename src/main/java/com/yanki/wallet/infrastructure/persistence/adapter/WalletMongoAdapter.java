package com.yanki.wallet.infrastructure.persistence.adapter;

import com.yanki.wallet.domain.model.Wallet;
import com.yanki.wallet.domain.port.out.WalletPersistencePort;
import com.yanki.wallet.infrastructure.persistence.mapper.WalletDocumentMapper;
import com.yanki.wallet.infrastructure.persistence.repository.SpringDataWalletRepository;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que implementa la persistencia de wallets sobre MongoDB Reactivo.
 *
 * <p>Convierte los tipos de Project Reactor (Mono) a RxJava 3 (Single/Maybe)
 * y traduce entre {@link Wallet} (dominio) y {@link com.yanki.wallet.infrastructure.persistence.document.WalletDocument} (persistencia).</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@Component
public class WalletMongoAdapter implements WalletPersistencePort {

    private final SpringDataWalletRepository repository;

    public WalletMongoAdapter(SpringDataWalletRepository repository) {
        this.repository = repository;
    }

    @Override
    public Maybe<Wallet> findById(String id) {
        log.debug("[MongoDB] Buscando wallet por ID: {}", id);
        return Maybe.fromPublisher(
                repository.findById(id).map(WalletDocumentMapper::toDomain)
        )
                .doOnSuccess(w -> log.debug("[MongoDB] Wallet encontrada: {} cliente: {} saldo: {}", w.getId(), w.getCustomerId(), w.getBalance()))
                .doOnError(error -> log.error("[MongoDB] Error buscando wallet por ID: {} - {}", id, error.getMessage(), error));
    }

    @Override
    public Maybe<Wallet> findByPhoneNumber(String phoneNumber) {
        log.debug("[MongoDB] Buscando wallet por teléfono: {}", phoneNumber);
        return Maybe.fromPublisher(
                repository.findByPhoneNumber(phoneNumber).map(WalletDocumentMapper::toDomain)
        )
                .doOnSuccess(w -> log.debug("[MongoDB] Wallet encontrada por teléfono: {} walletId: {}", phoneNumber, w.getId()))
                .doOnError(error -> log.error("[MongoDB] Error buscando wallet por teléfono: {} - {}", phoneNumber, error.getMessage(), error));
    }

    @Override
    public Single<Boolean> existsByCustomerId(String customerId) {
        log.debug("[MongoDB] Verificando existencia de wallet para customerId: {}", customerId);
        return Single.fromPublisher(
                repository.existsByCustomerId(customerId)
        )
                .doOnSuccess(exists -> log.debug("[MongoDB] Wallet existe para customerId: {} -> {}", customerId, exists))
                .doOnError(error -> log.error("[MongoDB] Error verificando existencia de wallet: customerId={} - {}", customerId, error.getMessage(), error));
    }

    @Override
    public Single<Wallet> save(Wallet wallet) {
        log.debug("[MongoDB] Guardando wallet: {} cliente: {} estado: {} saldo: {}",
                wallet.getId(), wallet.getCustomerId(), wallet.getStatus(), wallet.getBalance());
        return Single.fromPublisher(
                repository.save(WalletDocumentMapper.toDocument(wallet))
                        .map(WalletDocumentMapper::toDomain)
        )
                .doOnSuccess(w -> log.debug("[MongoDB] Wallet guardada exitosamente: {}", w.getId()))
                .doOnError(error -> log.error("[MongoDB] Error guardando wallet: {} - {}", wallet.getId(), error.getMessage(), error));
    }
}
