package com.yanki.wallet.infrastructure.persistence.repository;

import com.yanki.wallet.infrastructure.persistence.document.CustomerRiskViewDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repositorio Spring Data MongoDB reactivo para la vista de riesgo del cliente.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Repository
public interface SpringDataCustomerRiskViewRepository extends ReactiveMongoRepository<CustomerRiskViewDocument, String> {

    Mono<CustomerRiskViewDocument> findByCustomerId(String customerId);
}
