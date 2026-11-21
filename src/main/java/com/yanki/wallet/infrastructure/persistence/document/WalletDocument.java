package com.yanki.wallet.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Documento MongoDB para la colección de wallets.
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wallets")
public class WalletDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String customerId;

    @Indexed(unique = true)
    private String phoneNumber;

    private String imei;

    private BigDecimal balance;
    private String status;
    private String associatedDebitCardId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
