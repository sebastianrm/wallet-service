package com.yanki.wallet.domain.service;

import com.yanki.wallet.domain.event.Topics;
import com.yanki.wallet.domain.event.WalletOpenedEvent;
import com.yanki.wallet.domain.event.WalletTransactionEvent;
import com.yanki.wallet.domain.event.WalletTopupRequestedEvent;
import com.yanki.wallet.domain.event.WalletTopupSettledEvent;
import com.yanki.wallet.domain.exception.BusinessException;
import com.yanki.wallet.domain.exception.ErrorCodes;
import com.yanki.wallet.domain.model.CustomerRiskView;
import com.yanki.wallet.domain.model.Transaction;
import com.yanki.wallet.domain.model.TransactionStatus;
import com.yanki.wallet.domain.model.TransactionType;
import com.yanki.wallet.domain.model.Wallet;
import com.yanki.wallet.domain.model.WalletStatus;
import com.yanki.wallet.domain.port.in.AssociateCardUseCase;
import com.yanki.wallet.domain.port.in.DisassociateCardUseCase;
import com.yanki.wallet.domain.port.in.GetWalletUseCase;
import com.yanki.wallet.domain.port.in.OpenWalletUseCase;
import com.yanki.wallet.domain.port.in.SendP2PUseCase;
import com.yanki.wallet.domain.port.in.TopUpWalletUseCase;
import com.yanki.wallet.domain.port.in.WithdrawWalletUseCase;
import com.yanki.wallet.domain.port.out.CachePort;
import com.yanki.wallet.domain.port.out.CustomerRiskViewPort;
import com.yanki.wallet.domain.port.out.EventPublisherPort;
import com.yanki.wallet.domain.port.out.IdempotencyPort;
import com.yanki.wallet.domain.port.out.TransactionPersistencePort;
import com.yanki.wallet.domain.port.out.WalletPersistencePort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio principal para la gestión de wallets en la plataforma Yanki.
 *
 * <p>Implementa todos los casos de uso del monedero móvil utilizando
 * reactividad pura con RxJava 3, sin bloqueos.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Service
public class WalletService implements
        OpenWalletUseCase,
        TopUpWalletUseCase,
        SendP2PUseCase,
        WithdrawWalletUseCase,
        AssociateCardUseCase,
        DisassociateCardUseCase,
        GetWalletUseCase {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletPersistencePort walletPersistencePort;
    private final TransactionPersistencePort transactionPersistencePort;
    private final CustomerRiskViewPort riskViewPort;
    private final EventPublisherPort eventPublisherPort;
    private final IdempotencyPort idempotencyPort;
    private final CachePort cachePort;

    public WalletService(
            WalletPersistencePort walletPersistencePort,
            TransactionPersistencePort transactionPersistencePort,
            CustomerRiskViewPort riskViewPort,
            EventPublisherPort eventPublisherPort,
            IdempotencyPort idempotencyPort,
            CachePort cachePort
    ) {
        this.walletPersistencePort = walletPersistencePort;
        this.transactionPersistencePort = transactionPersistencePort;
        this.riskViewPort = riskViewPort;
        this.eventPublisherPort = eventPublisherPort;
        this.idempotencyPort = idempotencyPort;
        this.cachePort = cachePort;
    }

    // ==================== Open Wallet ====================

    @Override
    public Single<Wallet> execute(String customerId, String phoneNumber, String imei) {
        log.info("Iniciando apertura de wallet para cliente: {} telefono: {} imei: {}", customerId, phoneNumber, imei);
        log.debug("Verificando si ya existe wallet para customerId: {}", customerId);

        return Single.defer(() ->
                walletPersistencePort.existsByCustomerId(customerId)
                        .flatMap(exists -> {
                            if (exists) {
                                log.warn("Apertura de wallet rechazada: el cliente {} ya tiene una wallet activa", customerId);
                                return Single.error(new BusinessException(
                                        ErrorCodes.WALLET_ALREADY_EXISTS,
                                        "El cliente ya tiene una wallet activa"));
                            }
                            log.debug("No existe wallet previa para customerId: {}", customerId);
                            return Single.just(customerId);
                        })
                        .flatMap(cid -> {
                            log.debug("Verificando vista de riesgo para customerId: {}", cid);
                            return checkRiskView(cid)
                                    .flatMap(canOperate -> {
                                        if (!canOperate) {
                                            log.warn("Apertura de wallet rechazada: cliente {} tiene deuda vencida", cid);
                                            return Single.error(new BusinessException(
                                                    ErrorCodes.CUSTOMER_HAS_OVERDUE_DEBT,
                                                    "Cliente tiene deuda vencida, no puede abrir wallet"));
                                        }
                                        log.debug("Cliente {} no tiene deuda vencida, puede abrir wallet", cid);
                                        return Single.just(cid);
                                    });
                        })
                        .flatMap(cid -> {
                            Wallet wallet = Wallet.builder()
                                    .id(generateWalletId())
                                    .customerId(customerId)
                                    .phoneNumber(phoneNumber)
                                    .imei(imei)
                                    .balance(BigDecimal.ZERO)
                                    .status(WalletStatus.ACTIVE)
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build();
                            log.info("Creando wallet: {} para cliente: {} telefono: {}", wallet.getId(), customerId, phoneNumber);
                            return walletPersistencePort.save(wallet);
                        })
                        .flatMap(wallet -> {
                            log.info("Publicando evento wallet.opened.v1 para wallet: {}", wallet.getId());
                            return publishWalletOpened(wallet)
                                    .andThen(Single.just(wallet));
                        })
                        .doOnSuccess(w -> log.info("Wallet abierta exitosamente: {} para cliente: {} telefono: {}", w.getId(), customerId, phoneNumber))
                        .doOnError(error -> log.error("Error al abrir wallet para cliente: {} - {}", customerId, error.getMessage(), error))
                        .subscribeOn(Schedulers.io())
        );
    }

    // ==================== Top-Up ====================

    @Override
    public Single<Transaction> execute(String walletId, BigDecimal amount) {
        log.info("Iniciando top-up para wallet: {} monto: {}", walletId, amount);
        log.debug("Validando wallet y tarjeta de débito asociada para wallet: {}", walletId);

        return Single.defer(() ->
                getWalletOrError(walletId)
                        .flatMap(wallet -> {
                            if (wallet.getAssociatedDebitCardId() == null) {
                                log.warn("Top-up rechazado: wallet {} no tiene tarjeta de débito asociada", walletId);
                                throw new BusinessException(
                                        ErrorCodes.DEBIT_CARD_REQUIRED,
                                        "Se requiere tarjeta de débito asociada para realizar top-up");
                            }
                            log.debug("Wallet {} tiene tarjeta de débito asociada: {}", walletId, wallet.getAssociatedDebitCardId());
                            return Single.just(wallet);
                        })
                        .flatMap(wallet -> {
                            Transaction transaction = Transaction.builder()
                                    .id(generateTransactionId())
                                    .walletId(walletId)
                                    .type(TransactionType.TOPUP)
                                    .amount(amount.setScale(2, java.math.RoundingMode.HALF_UP))
                                    .balanceAfter(BigDecimal.ZERO)
                                    .status(TransactionStatus.PENDING)
                                    .referenceId(generateReferenceId())
                                    .description("Carga de saldo desde tarjeta débito")
                                    .occurredAt(LocalDateTime.now())
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            log.info("Registrando transacción top-up: {} wallet: {} monto: {} estado: PENDING",
                                    transaction.getId(), walletId, transaction.getAmount());
                            return transactionPersistencePort.save(transaction)
                                    .flatMap(tx -> {
                                        log.info("Publicando evento wallet.topup.requested.v1: {} wallet: {} monto: {}",
                                                tx.getId(), walletId, tx.getAmount());
                                        return publishTopupRequested(wallet, tx)
                                                .andThen(Single.just(tx));
                                    });
                        })
                        .doOnSuccess(tx -> log.info("Top-up solicitado exitosamente: {} wallet: {} monto: {}",
                                tx.getId(), walletId, amount))
                        .doOnError(error -> log.error("Error al solicitar top-up para wallet: {} monto: {} - {}",
                                walletId, amount, error.getMessage(), error))
                        .subscribeOn(Schedulers.io())
        );
    }

    /**
     * Procesa la respuesta de un top-up (consumido desde Kafka).
     */
    public Completable processTopupSettled(WalletTopupSettledEvent event) {
        log.info("Procesando evento topup.settled.v1: topupId={} walletId={} status={} amount={}",
                event.getTopupId(), event.getWalletId(), event.getStatus(), event.getAmount());

        return Completable.defer(() -> {
            String idempotencyKey = buildIdempotencyKey(
                    Topics.WALLET_TOPUP_SETTLED, event.getTopupId(), event.getOccurredAt());
            log.debug("Verificando idempotencia para key: {}", idempotencyKey);

            return idempotencyPort.checkAndMarkProcessed(idempotencyKey)
                    .flatMapCompletable(processed -> {
                        if (processed) {
                            log.warn("Evento topup.settled.v1 ya fue procesado (idempotente): topupId={}", event.getTopupId());
                            return Completable.complete();
                        }
                        log.debug("Evento topup.settled.v1 no procesado anteriormente, procediendo: topupId={}", event.getTopupId());
                        return transactionPersistencePort.findById(event.getTopupId())
                                .flatMapCompletable(transaction -> {
                                    log.debug("Transacción encontrada: {} wallet: {} tipo: {}",
                                            transaction.getId(), transaction.getWalletId(), transaction.getType());
                                    if ("SETTLED".equals(event.getStatus())) {
                                        log.info("Top-up confirmado como SETTLED: topupId={} walletId={} amount={}",
                                                event.getTopupId(), event.getWalletId(), event.getAmount());
                                        return walletPersistencePort.findById(event.getWalletId())
                                                .toSingle()
                                                .flatMap(wallet -> {
                                                    log.debug("Actualizando saldo de wallet: {} saldo actual: {} monto top-up: {}",
                                                            wallet.getId(), wallet.getBalance(), event.getAmount());
                                                    Wallet updated = wallet.topUp(event.getAmount());
                                                    log.info("Nuevo saldo de wallet: {} saldo: {}", updated.getId(), updated.getBalance());
                                                    return walletPersistencePort.save(updated)
                                                            .flatMap(w -> {
                                                                Wallet finalW = w;
                                                                log.debug("Invalidando y actualizando cache para wallet: {}", w.getId());
                                                                return cachePort.invalidateBalance(w.getId())
                                                                        .andThen(cachePort.setBalance(w.getId(), w.getBalance()))
                                                                        .toSingleDefault(finalW);
                                                            });
                                                })
                                                .ignoreElement()
                                                .andThen(updateTransactionStatus(
                                                        transaction, TransactionStatus.COMPLETED, event.getAmount()))
                                                .andThen(publishTransactionEvent(
                                                        transaction, event.getWalletId(),
                                                        TransactionType.TOPUP, event.getAmount()))
                                                .doOnComplete(() -> log.info("Top-up SETTLED procesado exitosamente: topupId={} walletId={}",
                                                        event.getTopupId(), event.getWalletId()))
                                                .doOnError(error -> log.error("Error procesando top-up SETTLED: topupId={} - {}",
                                                        event.getTopupId(), error.getMessage(), error));
                                    } else {
                                        log.warn("Top-up rechazado: topupId={} reason={}",
                                                event.getTopupId(), event.getReason());
                                        return updateTransactionStatus(
                                                transaction, TransactionStatus.REJECTED, BigDecimal.ZERO)
                                                .andThen(Completable.error(new BusinessException(
                                                        "TOPUP_REJECTED",
                                                        event.getReason() != null ? event.getReason() : "Top-up rechazado")));
                                    }
                                })
                                .onErrorResumeNext(error -> {
                                    log.error("Error buscando transacción para topup settled: topupId={} - {}",
                                            event.getTopupId(), error.getMessage(), error);
                                    return Completable.error(error);
                                });
                    })
                    .subscribeOn(Schedulers.io());
        });
    }

    // ==================== P2P Send ====================

    @Override
    public Single<Transaction> execute(String walletId, String recipientPhone, BigDecimal amount) {
        log.info("Iniciando envío P2P: wallet={} destinatario={} monto={}", walletId, recipientPhone, amount);
        log.debug("Buscando wallet origen: {} y wallet destino por teléfono: {}", walletId, recipientPhone);

        return Single.defer(() ->
                getWalletOrError(walletId)
                        .flatMap(sender ->
                                walletPersistencePort.findByPhoneNumber(recipientPhone)
                                        .toSingle()
                                        .flatMap(recipient -> {
                                            if (sender.getId().equals(recipient.getId())) {
                                                log.warn("Envío P2P rechazado: wallet {} intenta enviar a sí mismo (teléfono: {})",
                                                        walletId, recipientPhone);
                                                throw new BusinessException(
                                                        ErrorCodes.CANNOT_SEND_TO_SELF,
                                                        "No se puede enviar dinero a la misma wallet");
                                            }
                                            log.debug("Wallet destino encontrada: {} para teléfono: {}",
                                                    recipient.getId(), recipientPhone);
                                            return Single.just(recipient);
                                        })
                                        .onErrorResumeNext(throwable -> {
                                            if (throwable instanceof BusinessException
                                                    && ErrorCodes.CANNOT_SEND_TO_SELF.equals(((BusinessException) throwable).getCode())) {
                                                return Single.error(throwable);
                                            }
                                            log.warn("Envío P2P rechazado: no se encontró wallet para teléfono: {}", recipientPhone);
                                            return Single.error(new BusinessException(
                                                    ErrorCodes.RECIPIENT_NOT_FOUND,
                                                    "No se encontró wallet para el teléfono: " + recipientPhone));
                                        })
                        )
                        .flatMap(recipient -> {
                            Transaction sendTx = Transaction.builder()
                                    .id(generateTransactionId())
                                    .walletId(walletId)
                                    .type(TransactionType.SEND)
                                    .amount(amount.setScale(2, java.math.RoundingMode.HALF_UP))
                                    .balanceAfter(BigDecimal.ZERO)
                                    .status(TransactionStatus.COMPLETED)
                                    .counterpartyPhone(recipientPhone)
                                    .counterpartyWalletId(recipient.getId())
                                    .referenceId(generateReferenceId())
                                    .description("Envío P2P a " + recipientPhone)
                                    .occurredAt(LocalDateTime.now())
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            log.info("Procesando transacción P2P: {} origen={} destino={} monto={}",
                                    sendTx.getId(), walletId, recipient.getId(), sendTx.getAmount());
                            return getWalletOrError(walletId)
                                    .flatMap(sender -> {
                                        log.debug("Debitando wallet origen: {} monto: {} saldo actual: {}",
                                                walletId, sendTx.getAmount(), sender.getBalance());
                                        Wallet updatedSender = sender.debit(sendTx.getAmount());
                                        BigDecimal finalBalance = updatedSender.getBalance();
                                        log.info("Wallet origen debitada: {} nuevo saldo: {}", walletId, finalBalance);
                                        return walletPersistencePort.save(updatedSender)
                                                .flatMap(savedSender ->
                                                    cachePort.invalidateBalance(savedSender.getId())
                                                        .andThen(cachePort.setBalance(savedSender.getId(), savedSender.getBalance()))
                                                        .andThen(transactionPersistencePort.save(sendTx.toBuilder()
                                                                .balanceAfter(finalBalance)
                                                                .build()))
                                                );
                                    });
                        })
                        .doOnSuccess(tx -> log.info("Envío P2P completado exitosamente: {} wallet: {} destinatario: {} monto: {}",
                                tx.getId(), walletId, recipientPhone, tx.getAmount()))
                        .doOnError(error -> log.error("Error en envío P2P: wallet={} destinatario={} monto={} - {}",
                                walletId, recipientPhone, amount, error.getMessage(), error))
                        .subscribeOn(Schedulers.io())
        );
    }

    // ==================== Withdraw ====================

    @Override
    public Single<Transaction> withdraw(String walletId, BigDecimal amount) {
        log.info("Iniciando retiro: wallet={} monto={}", walletId, amount);
        log.debug("Validando saldo disponible para wallet: {} monto: {}", walletId, amount);

        return Single.defer(() ->
                getWalletOrError(walletId)
                        .flatMap(wallet -> {
                            log.debug("Debitando wallet: {} monto: {} saldo actual: {}",
                                    walletId, amount, wallet.getBalance());
                            Wallet updated = wallet.debit(amount);
                            log.info("Wallet debitada: {} nuevo saldo: {}", updated.getId(), updated.getBalance());
                            return walletPersistencePort.save(updated)
                                    .flatMap(saved -> {
                                        Wallet finalSaved = saved;
                                        log.debug("Invalidando y actualizando cache para wallet: {}", saved.getId());
                                        return cachePort.invalidateBalance(saved.getId())
                                                .andThen(cachePort.setBalance(saved.getId(), saved.getBalance()))
                                                .toSingleDefault(finalSaved);
                                    });
                        })
                        .flatMap(wallet -> {
                            Transaction transaction = Transaction.builder()
                                    .id(generateTransactionId())
                                    .walletId(walletId)
                                    .type(TransactionType.WITHDRAWAL)
                                    .amount(amount.setScale(2, java.math.RoundingMode.HALF_UP))
                                    .balanceAfter(wallet.getBalance())
                                    .status(TransactionStatus.COMPLETED)
                                    .referenceId(generateReferenceId())
                                    .description("Retiro a cuenta bancaria")
                                    .occurredAt(LocalDateTime.now())
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            log.info("Registrando transacción de retiro: {} wallet: {} monto: {}",
                                    transaction.getId(), walletId, transaction.getAmount());
                            return transactionPersistencePort.save(transaction)
                                    .flatMap(tx -> {
                                        log.info("Publicando evento wallet.transaction.registered.v1: {} tipo: WITHDRAWAL", tx.getId());
                                        Completable publish = publishTransactionEvent(
                                                tx, walletId, TransactionType.WITHDRAWAL, amount);
                                        return publish.andThen(Single.just(tx));
                                    });
                        })
                        .doOnSuccess(tx -> log.info("Retiro completado exitosamente: {} wallet: {} monto: {}",
                                tx.getId(), walletId, amount))
                        .doOnError(error -> log.error("Error en retiro: wallet={} monto={} - {}",
                                walletId, amount, error.getMessage(), error))
                        .subscribeOn(Schedulers.io())
        );
    }

    // ==================== Associate Card ====================

    @Override
    public Single<Wallet> execute(String walletId, String cardId) {
        log.info("Iniciando asociación de tarjeta: wallet={} cardId={}", walletId, cardId);
        log.debug("Verificando si la tarjeta {} ya está asociada a wallet: {}", cardId, walletId);

        return Single.defer(() ->
                getWalletOrError(walletId)
                        .flatMap(wallet -> {
                            if (cardId.equals(wallet.getAssociatedDebitCardId())) {
                                log.warn("Asociación de tarjeta rechazada: la tarjeta {} ya está asociada a wallet: {}",
                                        cardId, walletId);
                                throw new BusinessException(
                                        ErrorCodes.CARD_ALREADY_ASSOCIATED,
                                        "La tarjeta ya está asociada a esta wallet");
                            }
                            log.info("Asociando tarjeta: {} a wallet: {}", cardId, walletId);
                            Wallet updated = wallet.associateCard(cardId);
                            return walletPersistencePort.save(updated);
                        })
                        .doOnSuccess(w -> log.info("Tarjeta asociada exitosamente: {} a wallet: {}",
                                w.getAssociatedDebitCardId(), walletId))
                        .doOnError(error -> log.error("Error al asociar tarjeta: wallet={} cardId={} - {}",
                                walletId, cardId, error.getMessage(), error))
                        .subscribeOn(Schedulers.io())
        );
    }

    // ==================== Disassociate Card ====================

    @Override
    public Single<Wallet> execute(String walletId) {
        log.info("Iniciando desasociación de tarjeta de wallet: {}", walletId);
        log.debug("Verificando si wallet: {} tiene tarjeta asociada", walletId);

        return Single.defer(() ->
                getWalletOrError(walletId)
                        .flatMap(wallet -> {
                            if (wallet.getAssociatedDebitCardId() == null) {
                                log.warn("Desasociación de tarjeta rechazada: wallet {} no tiene tarjeta asociada", walletId);
                                throw new BusinessException(
                                        ErrorCodes.NO_CARD_ASSOCIATED,
                                        "No hay tarjeta asociada para desasociar");
                            }
                            log.info("Desasociando tarjeta: {} de wallet: {}", wallet.getAssociatedDebitCardId(), walletId);
                            Wallet updated = wallet.disassociateCard();
                            return walletPersistencePort.save(updated);
                        })
                        .doOnSuccess(w -> log.info("Tarjeta desasociada exitosamente de wallet: {}", walletId))
                        .doOnError(error -> log.error("Error al desasociar tarjeta de wallet: {} - {}",
                                walletId, error.getMessage(), error))
                        .subscribeOn(Schedulers.io())
        );
    }

    // ==================== Get Wallet ====================

    @Override
    public Maybe<Wallet> findById(String walletId) {
        log.debug("Buscando wallet por ID: {}", walletId);
        return walletPersistencePort.findById(walletId)
                .doOnSuccess(w -> log.debug("Wallet encontrada: {} cliente: {} saldo: {}",
                        w.getId(), w.getCustomerId(), w.getBalance()))
                .doOnError(error -> log.error("Error buscando wallet por ID: {} - {}", walletId, error.getMessage(), error))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Maybe<Wallet> findByPhoneNumber(String phoneNumber) {
        log.debug("Buscando wallet por teléfono: {}", phoneNumber);
        return walletPersistencePort.findByPhoneNumber(phoneNumber)
                .doOnSuccess(w -> log.debug("Wallet encontrada por teléfono: {} walletId: {}",
                        phoneNumber, w.getId()))
                .doOnError(error -> log.error("Error buscando wallet por teléfono: {} - {}", phoneNumber, error.getMessage(), error))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<Transaction> getTransactions(String walletId) {
        log.debug("Obteniendo transacciones para wallet: {}", walletId);
        return transactionPersistencePort.findByWalletId(walletId)
                .doOnError(error -> log.error("Error obteniendo transacciones para wallet: {} - {}",
                        walletId, error.getMessage(), error))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<Wallet> listAll() {
        log.debug("Listando todas las wallets");
        return walletPersistencePort.findAll()
                .doOnError(error -> log.error("Error listando wallets: {}", error.getMessage(), error))
                .subscribeOn(Schedulers.io());
    }

    // ==================== Risk & Blocking ====================

    /**
     * Procesa evento de deuda vencida y bloquea la wallet si aplica.
     */
    public Completable processCreditOverdue(String customerId, Boolean hasOverdueDebt) {
        log.info("Procesando evento credit.overdue.v1: customerId={} hasOverdueDebt={}", customerId, hasOverdueDebt);

        return Completable.defer(() ->
                riskViewPort.findByCustomerId(customerId)
                        .flatMap(view -> {
                            log.debug("Vista de riesgo existente para customerId: {} hasDebt: {}",
                                    customerId, view.getHasOverdueDebt());
                            return riskViewPort.save(
                                    CustomerRiskView.builder()
                                            .customerId(customerId)
                                            .hasOverdueDebt(hasOverdueDebt)
                                            .updatedAt(LocalDateTime.now())
                                            .build());
                        })
                        .onErrorReturnItem(CustomerRiskView.builder().customerId(customerId).build())
                        .ignoreElement()
                        .andThen(
                                walletPersistencePort.existsByCustomerId(customerId)
                                        .flatMapCompletable(hasWallet -> {
                                            if (Boolean.TRUE.equals(hasOverdueDebt) && hasWallet) {
                                                log.warn("Cliente {} tiene deuda vencida y wallet activa: bloqueando wallet", customerId);
                                                return walletPersistencePort.findById("")
                                                        .toSingle()
                                                        .flatMapCompletable(wallet -> {
                                                            Wallet blocked = wallet.block();
                                                            log.info("Wallet bloqueada por deuda vencida: {} cliente: {}",
                                                                    blocked.getId(), customerId);
                                                            return walletPersistencePort.save(blocked)
                                                                    .ignoreElement();
                                                        })
                                                        .onErrorComplete();
                                            } else if (Boolean.FALSE.equals(hasOverdueDebt) && hasWallet) {
                                                log.info("Cliente {} ya no tiene deuda vencida: wallet puede desbloquearse", customerId);
                                            }
                                            return Completable.complete();
                                        })
                        )
                        .doOnComplete(() -> log.info("Evento credit.overdue.v1 procesado exitosamente: customerId={}", customerId))
                        .doOnError(error -> log.error("Error procesando credit.overdue.v1: customerId={} - {}",
                                customerId, error.getMessage(), error))
                        .subscribeOn(Schedulers.io())
        );
    }

    // ==================== Private Helpers ====================

    private Single<Wallet> getWalletOrError(String walletId) {
        return walletPersistencePort.findById(walletId)
                .toSingle()
                .onErrorResumeNext(throwable -> {
                    log.warn("Wallet no encontrada: {}", walletId);
                    return Single.error(new BusinessException(
                            ErrorCodes.WALLET_NOT_FOUND,
                            "Wallet no encontrada: " + walletId));
                });
    }

    private Single<Boolean> checkRiskView(String customerId) {
        return riskViewPort.findByCustomerId(customerId)
                .map(view -> {
                    boolean canOperate = view.canOperate();
                    log.debug("Vista de riesgo para customerId: {} canOperate: {}", customerId, canOperate);
                    return canOperate;
                })
                .onErrorResumeNext(throwable -> {
                    log.debug("No se encontró vista de riesgo para customerId: {} (asumiendo sin riesgo)", customerId);
                    return Single.just(true);
                });
    }

    private Completable publishWalletOpened(Wallet wallet) {
        log.debug("Construyendo evento wallet.opened.v1: walletId={} customerId={}",
                wallet.getId(), wallet.getCustomerId());
        WalletOpenedEvent event = WalletOpenedEvent.builder()
                .walletId(wallet.getId())
                .customerId(wallet.getCustomerId())
                .phoneNumber(wallet.getPhoneNumber())
                .occurredAt(LocalDateTime.now())
                .build();
        return eventPublisherPort.publishWalletOpened(event)
                .doOnComplete(() -> log.debug("Evento wallet.opened.v1 publicado: walletId={}", wallet.getId()))
                .doOnError(error -> log.error("Error publicando wallet.opened.v1: walletId={} - {}",
                        wallet.getId(), error.getMessage(), error));
    }

    private Completable publishTopupRequested(Wallet wallet, Transaction transaction) {
        log.debug("Construyendo evento wallet.topup.requested.v1: topupId={} walletId={} amount={}",
                transaction.getId(), wallet.getId(), transaction.getAmount());
        WalletTopupRequestedEvent event = WalletTopupRequestedEvent.builder()
                .topupId(transaction.getId())
                .walletId(wallet.getId())
                .customerId(wallet.getCustomerId())
                .debitCardId(wallet.getAssociatedDebitCardId())
                .amount(transaction.getAmount())
                .occurredAt(LocalDateTime.now())
                .build();
        return eventPublisherPort.publishTopupRequested(event)
                .doOnComplete(() -> log.debug("Evento wallet.topup.requested.v1 publicado: topupId={}", transaction.getId()))
                .doOnError(error -> log.error("Error publicando wallet.topup.requested.v1: topupId={} - {}",
                        transaction.getId(), error.getMessage(), error));
    }

    private Completable publishTransactionEvent(
            Transaction transaction, String walletId, TransactionType type, BigDecimal amount) {
        log.debug("Construyendo evento wallet.transaction.registered.v1: txId={} walletId={} type={}",
                transaction.getId(), walletId, type);
        WalletTransactionEvent event = WalletTransactionEvent.builder()
                .transactionId(transaction.getId())
                .walletId(walletId)
                .type(type.name())
                .amount(amount)
                .balanceAfter(transaction.getBalanceAfter())
                .status(transaction.getStatus().name())
                .counterpartyPhone(transaction.getCounterpartyPhone())
                .counterpartyWalletId(transaction.getCounterpartyWalletId())
                .referenceId(transaction.getReferenceId())
                .occurredAt(LocalDateTime.now())
                .build();
        return eventPublisherPort.publishTransactionRegistered(event)
                .doOnComplete(() -> log.debug("Evento wallet.transaction.registered.v1 publicado: txId={} type={}",
                        transaction.getId(), type))
                .doOnError(error -> log.error("Error publicando wallet.transaction.registered.v1: txId={} - {}",
                        transaction.getId(), error.getMessage(), error));
    }

    private Completable updateTransactionStatus(
            Transaction transaction, TransactionStatus status, BigDecimal balanceAfter) {
        log.debug("Actualizando estado de transacción: {} nuevo estado: {} balanceAfter: {}",
                transaction.getId(), status, balanceAfter);
        Transaction updated = transaction.toBuilder()
                .status(status)
                .balanceAfter(balanceAfter)
                .build();
        return transactionPersistencePort.updateStatus(updated)
                .doOnComplete(() -> log.debug("Estado de transacción actualizado: {} -> {}",
                        transaction.getId(), status))
                .doOnError(error -> log.error("Error actualizando estado de transacción: {} - {}",
                        transaction.getId(), error.getMessage(), error));
    }

    private String buildIdempotencyKey(String topic, String entityId, LocalDateTime occurredAt) {
        String occurredAtStr = occurredAt != null ? occurredAt.toString() : "no-date";
        return topic + ":" + entityId + ":" + occurredAtStr;
    }

    private String generateWalletId() {
        return "WALLET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String generateTransactionId() {
        return "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String generateReferenceId() {
        return "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
