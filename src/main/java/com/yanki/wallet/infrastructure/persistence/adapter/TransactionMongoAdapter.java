package com.yanki.wallet.infrastructure.persistence.adapter;

import com.yanki.wallet.domain.model.Transaction;
import com.yanki.wallet.domain.port.out.TransactionPersistencePort;
import com.yanki.wallet.infrastructure.persistence.mapper.TransactionDocumentMapper;
import com.yanki.wallet.infrastructure.persistence.repository.SpringDataTransactionRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que implementa la persistencia de transacciones sobre MongoDB Reactivo.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@Component
public class TransactionMongoAdapter implements TransactionPersistencePort {

    private final SpringDataTransactionRepository repository;

    public TransactionMongoAdapter(SpringDataTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Single<Transaction> save(Transaction transaction) {
        log.debug("[MongoDB] Guardando transacción: {} wallet: {} tipo: {} monto: {} estado: {}",
                transaction.getId(), transaction.getWalletId(), transaction.getType(), transaction.getAmount(), transaction.getStatus());
        return Single.fromPublisher(
                repository.save(TransactionDocumentMapper.toDocument(transaction))
                        .map(TransactionDocumentMapper::toDomain)
        )
                .doOnSuccess(tx -> log.debug("[MongoDB] Transacción guardada: {}", tx.getId()))
                .doOnError(error -> log.error("[MongoDB] Error guardando transacción: {} - {}", transaction.getId(), error.getMessage(), error));
    }

    @Override
    public Flowable<Transaction> findByWalletId(String walletId) {
        log.debug("[MongoDB] Buscando transacciones para wallet: {}", walletId);
        return Flowable.fromPublisher(
                repository.findByWalletIdOrderByOccurredAtDesc(walletId)
                        .map(TransactionDocumentMapper::toDomain)
        )
                .doOnError(error -> log.error("[MongoDB] Error buscando transacciones para wallet: {} - {}", walletId, error.getMessage(), error));
    }

    @Override
    public Single<Transaction> findById(String id) {
        log.debug("[MongoDB] Buscando transacción por ID: {}", id);
        return Single.fromPublisher(
                repository.findById(id).map(TransactionDocumentMapper::toDomain)
        )
                .doOnSuccess(tx -> log.debug("[MongoDB] Transacción encontrada: {} tipo: {} monto: {}", tx.getId(), tx.getType(), tx.getAmount()))
                .doOnError(error -> log.error("[MongoDB] Error buscando transacción por ID: {} - {}", id, error.getMessage(), error));
    }

    @Override
    public Completable updateStatus(Transaction transaction) {
        log.debug("[MongoDB] Actualizando estado de transacción: {} nuevo estado: {}", transaction.getId(), transaction.getStatus());
        return Completable.fromPublisher(
                repository.save(TransactionDocumentMapper.toDocument(transaction))
        )
                .doOnComplete(() -> log.debug("[MongoDB] Estado de transacción actualizado: {} -> {}", transaction.getId(), transaction.getStatus()))
                .doOnError(error -> log.error("[MongoDB] Error actualizando estado de transacción: {} - {}", transaction.getId(), error.getMessage(), error));
    }
}
