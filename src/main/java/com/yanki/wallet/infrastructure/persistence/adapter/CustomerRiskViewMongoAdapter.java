package com.yanki.wallet.infrastructure.persistence.adapter;

import com.yanki.wallet.domain.model.CustomerRiskView;
import com.yanki.wallet.domain.port.out.CustomerRiskViewPort;
import com.yanki.wallet.infrastructure.persistence.document.CustomerRiskViewDocument;
import com.yanki.wallet.infrastructure.persistence.repository.SpringDataCustomerRiskViewRepository;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que implementa la vista de riesgo del cliente sobre MongoDB Reactivo.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@Component
public class CustomerRiskViewMongoAdapter implements CustomerRiskViewPort {

    private final SpringDataCustomerRiskViewRepository repository;

    public CustomerRiskViewMongoAdapter(SpringDataCustomerRiskViewRepository repository) {
        this.repository = repository;
    }

    @Override
    public Single<CustomerRiskView> findByCustomerId(String customerId) {
        log.debug("[MongoDB] Buscando vista de riesgo para customerId: {}", customerId);
        return Single.fromPublisher(
                repository.findByCustomerId(customerId).map(this::toDomain)
        )
                .doOnSuccess(view -> log.debug("[MongoDB] Vista de riesgo encontrada: customerId={} hasDebt={}",
                        customerId, view.getHasOverdueDebt()))
                .doOnError(error -> log.error("[MongoDB] Error buscando vista de riesgo: customerId={} - {}",
                        customerId, error.getMessage(), error));
    }

    @Override
    public Single<CustomerRiskView> save(CustomerRiskView view) {
        log.debug("[MongoDB] Guardando vista de riesgo: customerId={} hasDebt={}",
                view.getCustomerId(), view.getHasOverdueDebt());
        return Single.fromPublisher(
                repository.save(toDocument(view)).map(this::toDomain)
        )
                .doOnSuccess(saved -> log.debug("[MongoDB] Vista de riesgo guardada: customerId={}", saved.getCustomerId()))
                .doOnError(error -> log.error("[MongoDB] Error guardando vista de riesgo: customerId={} - {}",
                        view.getCustomerId(), error.getMessage(), error));
    }

    private CustomerRiskView toDomain(CustomerRiskViewDocument document) {
        return CustomerRiskView.builder()
                .customerId(document.getCustomerId())
                .hasOverdueDebt(document.getHasOverdueDebt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private CustomerRiskViewDocument toDocument(CustomerRiskView view) {
        return CustomerRiskViewDocument.builder()
                .customerId(view.getCustomerId())
                .hasOverdueDebt(view.getHasOverdueDebt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }
}
