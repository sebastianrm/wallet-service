package com.yanki.wallet.infrastructure.persistence.adapter;

import com.yanki.wallet.domain.port.out.IdempotencyPort;
import com.yanki.wallet.infrastructure.persistence.document.ProcessedEventDocument;
import com.yanki.wallet.infrastructure.persistence.repository.SpringDataProcessedEventRepository;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que implementa el control de idempotencia sobre MongoDB.
 *
 * <p>Verifica si un evento Kafka ya fue procesado usando la clave
 * {@code <topic>:<entityId>:<occurredAt>} almacenada en la colección
 * {@code processed_events}.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@Component
public class IdempotencyMongoAdapter implements IdempotencyPort {

    private final SpringDataProcessedEventRepository repository;

    public IdempotencyMongoAdapter(SpringDataProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Single<Boolean> checkAndMarkProcessed(String idempotencyKey) {
        log.debug("[MongoDB] Verificando idempotencia: key={}", idempotencyKey);
        return Single.fromPublisher(
                repository.findById(idempotencyKey)
                        .defaultIfEmpty(null)
                        .map(existing -> {
                            if (existing == null) {
                                log.debug("[MongoDB] Evento no procesado anteriormente, marcando: key={}", idempotencyKey);
                                ProcessedEventDocument doc = ProcessedEventDocument.builder()
                                        .id(idempotencyKey)
                                        .build();
                                repository.save(doc).subscribe(
                                        saved -> log.debug("[MongoDB] Evento marcado como procesado: key={}", idempotencyKey),
                                        error -> log.error("[MongoDB] Error marcando evento como procesado: key={} - {}",
                                                idempotencyKey, error.getMessage(), error)
                                );
                                return false;
                            }
                            log.warn("[MongoDB] Evento ya fue procesado (idempotente): key={}", idempotencyKey);
                            return true;
                        })
        )
        .doOnError(error -> log.error("[MongoDB] Error verificando idempotencia: key={} - {}",
                idempotencyKey, error.getMessage(), error));
    }
}
