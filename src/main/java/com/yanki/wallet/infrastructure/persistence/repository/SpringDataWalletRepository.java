package com.yanki.wallet.infrastructure.persistence.repository;

import com.yanki.wallet.infrastructure.persistence.document.WalletDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repositorio Spring Data MongoDB reactivo para wallets.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Repository
public interface SpringDataWalletRepository extends ReactiveMongoRepository<WalletDocument, String> {

    Mono<WalletDocument> findByCustomerId(String customerId);

    Mono<WalletDocument> findByPhoneNumber(String phoneNumber);

    Mono<Boolean> existsByCustomerId(String customerId);
}
