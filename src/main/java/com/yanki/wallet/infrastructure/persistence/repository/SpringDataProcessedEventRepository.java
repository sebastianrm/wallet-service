package com.yanki.wallet.infrastructure.persistence.repository;

import com.yanki.wallet.infrastructure.persistence.document.ProcessedEventDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data MongoDB reactivo para eventos procesados (idempotencia).
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Repository
public interface SpringDataProcessedEventRepository extends ReactiveMongoRepository<ProcessedEventDocument, String> {
}
