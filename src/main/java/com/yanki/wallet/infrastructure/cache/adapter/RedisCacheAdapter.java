package com.yanki.wallet.infrastructure.cache.adapter;

import com.yanki.wallet.domain.port.out.CachePort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Adaptador de salida que implementa el caché Redis para wallets.
 *
 * <p>Almacena saldos con TTL de 5 minutos y mapeo teléfono→walletId con TTL de 24 horas.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Slf4j
@Component
public class RedisCacheAdapter implements CachePort {

    private static final String BALANCE_PREFIX = "yanki:wallet:balance:";
    private static final String PHONE_PREFIX = "yanki:wallet:phone:";
    private static final Duration BALANCE_TTL = Duration.ofMinutes(5);
    private static final Duration PHONE_TTL = Duration.ofHours(24);

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisCacheAdapter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Maybe<BigDecimal> getBalance(String walletId) {
        log.debug("[Redis] Obteniendo saldo en cache para wallet: {}", walletId);
        return Maybe.fromPublisher(
                redisTemplate.opsForValue().get(BALANCE_PREFIX + walletId)
                        .map(BigDecimal::new)
        )
                .doOnSuccess(balance -> log.debug("[Redis] Saldo encontrado en cache: wallet={} saldo={}", walletId, balance))
                .doOnError(error -> log.error("[Redis] Error obteniendo saldo en cache: wallet={} - {}", walletId, error.getMessage(), error));
    }

    @Override
    public Completable setBalance(String walletId, BigDecimal balance) {
        log.debug("[Redis] Estableciendo saldo en cache: wallet={} saldo={}", walletId, balance);
        return Completable.fromPublisher(
                redisTemplate.opsForValue().set(BALANCE_PREFIX + walletId, balance.toString(), BALANCE_TTL).then()
        )
                .doOnComplete(() -> log.debug("[Redis] Saldo establecido en cache: wallet={}", walletId))
                .doOnError(error -> log.error("[Redis] Error estableciendo saldo en cache: wallet={} - {}", walletId, error.getMessage(), error));
    }

    @Override
    public Completable invalidateBalance(String walletId) {
        log.debug("[Redis] Invalidando saldo en cache para wallet: {}", walletId);
        return Completable.fromPublisher(
                redisTemplate.delete(BALANCE_PREFIX + walletId).then()
        )
                .doOnComplete(() -> log.debug("[Redis] Saldo invalidado en cache: wallet={}", walletId))
                .doOnError(error -> log.error("[Redis] Error invalidando saldo en cache: wallet={} - {}", walletId, error.getMessage(), error));
    }

    @Override
    public Maybe<String> getWalletIdByPhone(String phoneNumber) {
        log.debug("[Redis] Buscando walletId por teléfono en cache: {}", phoneNumber);
        return Maybe.fromPublisher(
                redisTemplate.opsForValue().get(PHONE_PREFIX + phoneNumber)
        )
                .doOnSuccess(walletId -> log.debug("[Redis] WalletId encontrado en cache: phone={} walletId={}", phoneNumber, walletId))
                .doOnError(error -> log.error("[Redis] Error buscando walletId por teléfono: phone={} - {}", phoneNumber, error.getMessage(), error));
    }

    @Override
    public Single<Void> setWalletIdByPhone(String phoneNumber, String walletId) {
        log.debug("[Redis] Estableciendo mapeo teléfono→walletId: phone={} walletId={}", phoneNumber, walletId);
        return Single.fromPublisher(
                redisTemplate.opsForValue().set(PHONE_PREFIX + phoneNumber, walletId, PHONE_TTL).then()
        )
                .doOnError(error -> log.error("[Redis] Error estableciendo mapeo teléfono→walletId: phone={} - {}", phoneNumber, error.getMessage(), error));
    }
}
