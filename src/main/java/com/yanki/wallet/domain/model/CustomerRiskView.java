package com.yanki.wallet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Vista local del riesgo del cliente, alimentada por eventos Kafka.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRiskView {
    private String customerId;
    private Boolean hasOverdueDebt;
    private java.time.LocalDateTime updatedAt;

    public boolean canOperate() {
        return hasOverdueDebt == null || !hasOverdueDebt;
    }
}
