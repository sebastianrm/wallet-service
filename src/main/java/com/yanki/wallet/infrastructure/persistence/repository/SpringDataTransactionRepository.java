package com.yanki.wallet.infrastructure.persistence.repository;

import com.yanki.wallet.infrastructure.persistence.document.TransactionDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repositorio Spring Data MongoDB reactivo para transacciones.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Repository
public interface SpringDataTransactionRepository extends ReactiveMongoRepository<TransactionDocument, String> {

    Flux<TransactionDocument> findByWalletIdOrderByOccurredAtDesc(String walletId);
}
