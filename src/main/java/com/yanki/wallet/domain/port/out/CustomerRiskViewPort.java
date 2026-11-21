package com.yanki.wallet.domain.port.out;
import com.yanki.wallet.domain.model.CustomerRiskView;
import io.reactivex.rxjava3.core.Single;
/** Puerto de salida para consultar la vista de riesgo del cliente. */
public interface CustomerRiskViewPort {
    Single<CustomerRiskView> findByCustomerId(String customerId);
    Single<CustomerRiskView> save(CustomerRiskView view);
}
