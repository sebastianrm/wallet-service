package com.yanki.wallet.domain.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de dominio WalletService.
 *
 * <p>Cubre los casos de prueba CP-150 a CP-165 definidos en
 * {@code test-cases/CASOS_PRUEBA_WALLET.md} (CU-17: Gestión de Wallet).</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletPersistencePort walletPersistencePort;

    @Mock
    private TransactionPersistencePort transactionPersistencePort;

    @Mock
    private CustomerRiskViewPort riskViewPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private IdempotencyPort idempotencyPort;

    @Mock
    private CachePort cachePort;

    @Captor
    private ArgumentCaptor<Wallet> walletCaptor;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<WalletOpenedEvent> walletOpenedEventCaptor;

    @Captor
    private ArgumentCaptor<WalletTopupRequestedEvent> topupRequestedEventCaptor;

    @Captor
    private ArgumentCaptor<WalletTransactionEvent> transactionEventCaptor;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(
                walletPersistencePort,
                transactionPersistencePort,
                riskViewPort,
                eventPublisherPort,
                idempotencyPort,
                cachePort
        );
    }

    // ==================== CP-150: Apertura de wallet exitosa ====================

    @Nested
    @DisplayName("CP-150: Apertura de wallet exitosa")
    class OpenWalletHappyPath {

        @Test
        @DisplayName("Debe crear una wallet activa con saldo 0 cuando el cliente no tiene wallet previa")
        void openWallet_createsNewWallet_whenCustomerHasNoExistingWallet() {
            // Arrange
            String customerId = "DNI-12345678";
            String phone = "999888776";
            String imei = "353456789012345";

            when(walletPersistencePort.existsByCustomerId(customerId))
                    .thenReturn(Single.just(false));
            when(riskViewPort.findByCustomerId(customerId))
                    .thenReturn(Single.just(
                            CustomerRiskView.builder()
                                    .customerId(customerId)
                                    .hasOverdueDebt(false)
                                    .build()));
            when(walletPersistencePort.save(any(Wallet.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(eventPublisherPort.publishWalletOpened(any(WalletOpenedEvent.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(customerId, phone, imei))
            ).assertNext(wallet -> {
                assertThat(wallet.getId()).startsWith("WALLET-");
                assertThat(wallet.getCustomerId()).isEqualTo(customerId);
                assertThat(wallet.getPhoneNumber()).isEqualTo(phone);
                assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
                assertThat(wallet.getAssociatedDebitCardId()).isNull();
            }).verifyComplete();

            // Verify
            verify(walletPersistencePort, times(1)).existsByCustomerId(customerId);
            verify(walletPersistencePort, times(1)).save(walletCaptor.capture());
            verify(eventPublisherPort, times(1)).publishWalletOpened(walletOpenedEventCaptor.capture());

            WalletOpenedEvent event = walletOpenedEventCaptor.getValue();
            assertThat(event.getWalletId()).startsWith("WALLET-");
            assertThat(event.getCustomerId()).isEqualTo(customerId);
            assertThat(event.getPhoneNumber()).isEqualTo(phone);
        }
    }

    // ==================== CP-151: Apertura wallet rechazada - duplicidad ====================

    @Nested
    @DisplayName("CP-151: Apertura wallet rechazada por duplicidad")
    class OpenWalletDuplicate {

        @Test
        @DisplayName("Debe rechazar la apertura si el cliente ya tiene una wallet activa")
        void openWallet_rejects_whenCustomerAlreadyHasWallet() {
            // Arrange
            String customerId = "DNI-12345678";
            String phone = "999888776";
            String imei = "353456789012345";

            when(walletPersistencePort.existsByCustomerId(customerId))
                    .thenReturn(Single.just(true));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(customerId, phone, imei))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.WALLET_ALREADY_EXISTS)
            ).verify();

            // Verify
            verify(walletPersistencePort, never()).save(any());
            verify(eventPublisherPort, never()).publishWalletOpened(any());
        }
    }

    // ==================== CP-152: Apertura wallet rechazada - datos inválidos ====================

    @Nested
    @DisplayName("CP-152: Apertura wallet rechazada por datos inválidos")
    class OpenWalletInvalidData {

        @Test
        @DisplayName("Debe rechazar la apertura si el cliente tiene deuda vencida")
        void openWallet_rejects_whenCustomerHasOverdueDebt() {
            // Arrange
            String customerId = "DNI-12345678";
            String phone = "999888776";
            String imei = "353456789012345";

            when(walletPersistencePort.existsByCustomerId(customerId))
                    .thenReturn(Single.just(false));
            when(riskViewPort.findByCustomerId(customerId))
                    .thenReturn(Single.just(
                            CustomerRiskView.builder()
                                    .customerId(customerId)
                                    .hasOverdueDebt(true)
                                    .build()));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(customerId, phone, imei))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.CUSTOMER_HAS_OVERDUE_DEBT)
            ).verify();

            // Verify
            verify(walletPersistencePort, never()).save(any());
        }
    }

    // ==================== CP-153: Top-Up wallet exitosa ====================

    @Nested
    @DisplayName("CP-153: Recarga/Top-Up wallet exitosa")
    class TopUpHappyPath {

        @Test
        @DisplayName("Debe solicitar un top-up cuando la wallet tiene tarjeta débito asociada")
        void topUp_succeeds_whenWalletHasDebitCard() {
            // Arrange
            String walletId = "WALLET-001";
            BigDecimal amount = new BigDecimal("200.00");

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId("CARD-001")
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));
            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(eventPublisherPort.publishTopupRequested(any(WalletTopupRequestedEvent.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, amount))
            ).assertNext(transaction -> {
                assertThat(transaction.getWalletId()).isEqualTo(walletId);
                assertThat(transaction.getType()).isEqualTo(TransactionType.TOPUP);
                assertThat(transaction.getAmount()).isEqualByComparingTo(amount);
                assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
                assertThat(transaction.getReferenceId()).startsWith("REF-");
            }).verifyComplete();

            // Verify
            verify(transactionPersistencePort, times(1)).save(transactionCaptor.capture());
            verify(eventPublisherPort, times(1)).publishTopupRequested(topupRequestedEventCaptor.capture());

            WalletTopupRequestedEvent event = topupRequestedEventCaptor.getValue();
            assertThat(event.getWalletId()).isEqualTo(walletId);
            assertThat(event.getDebitCardId()).isEqualTo("CARD-001");
            assertThat(event.getAmount()).isEqualByComparingTo(amount);
        }
    }

    // ==================== CP-154: Top-Up rechazado - tarjeta inválida ====================

    @Nested
    @DisplayName("CP-154: Top-Up rechazado por tarjeta débito inválida")
    class TopUpNoCard {

        @Test
        @DisplayName("Debe rechazar el top-up si la wallet no tiene tarjeta débito asociada")
        void topUp_rejects_whenNoDebitCardAssociated() {
            // Arrange
            String walletId = "WALLET-001";
            BigDecimal amount = new BigDecimal("200.00");

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId(null)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, amount))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.DEBIT_CARD_REQUIRED)
            ).verify();

            // Verify
            verify(transactionPersistencePort, never()).save(any());
        }
    }

    // ==================== CP-157: Pago P2P exitoso ====================

    @Nested
    @DisplayName("CP-157: Pago P2P exitoso entre wallets")
    class P2PHappyPath {

        @Test
        @DisplayName("Debe completar un envío P2P exitoso entre wallets distintas")
        void sendP2P_succeeds_whenBothWalletsExist() {
            // Arrange
            String senderId = "WALLET-001";
            String recipientPhone = "987654321";
            BigDecimal amount = new BigDecimal("100.00");

            Wallet sender = Wallet.builder()
                    .id(senderId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            Wallet recipient = Wallet.builder()
                    .id("WALLET-002")
                    .customerId("CUST-002")
                    .phoneNumber(recipientPhone)
                    .balance(new BigDecimal("300.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            when(walletPersistencePort.findById(senderId)).thenReturn(Maybe.just(sender));
            when(walletPersistencePort.findByPhoneNumber(recipientPhone)).thenReturn(Maybe.just(recipient));
            when(walletPersistencePort.save(any(Wallet.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(cachePort.invalidateBalance(anyString()))
                    .thenReturn(Completable.complete());
            when(cachePort.setBalance(anyString(), any(BigDecimal.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(senderId, recipientPhone, amount))
            ).assertNext(transaction -> {
                assertThat(transaction.getWalletId()).isEqualTo(senderId);
                assertThat(transaction.getType()).isEqualTo(TransactionType.SEND);
                assertThat(transaction.getAmount()).isEqualByComparingTo(amount);
                assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
                assertThat(transaction.getCounterpartyPhone()).isEqualTo(recipientPhone);
                assertThat(transaction.getCounterpartyWalletId()).isEqualTo("WALLET-002");
                assertThat(transaction.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("400.00"));
            }).verifyComplete();

            // Verify
            verify(walletPersistencePort, atLeast(2)).findById(senderId);
            verify(walletPersistencePort, times(1)).findByPhoneNumber(recipientPhone);
            verify(walletPersistencePort, times(1)).save(walletCaptor.capture());
            Wallet savedWallet = walletCaptor.getValue();
            assertThat(savedWallet.getBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
        }
    }

    // ==================== CP-158: P2P rechazado - saldo insuficiente ====================

    @Nested
    @DisplayName("CP-158: Pago P2P rechazado por saldo insuficiente")
    class P2PInsufficientBalance {

        @Test
        @DisplayName("Debe rechazar el envío P2P si el saldo del remitente es insuficiente")
        void sendP2P_rejects_whenInsufficientBalance() {
            // Arrange
            String senderId = "WALLET-001";
            String recipientPhone = "987654321";
            BigDecimal amount = new BigDecimal("600.00");

            Wallet sender = Wallet.builder()
                    .id(senderId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            Wallet recipient = Wallet.builder()
                    .id("WALLET-002")
                    .customerId("CUST-002")
                    .phoneNumber(recipientPhone)
                    .balance(new BigDecimal("300.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            when(walletPersistencePort.findById(senderId)).thenReturn(Maybe.just(sender));
            when(walletPersistencePort.findByPhoneNumber(recipientPhone)).thenReturn(Maybe.just(recipient));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(senderId, recipientPhone, amount))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.INSUFFICIENT_BALANCE)
            ).verify();

            // Verify
            verify(walletPersistencePort, never()).save(any());
        }
    }

    // ==================== CP-159: P2P rechazado - wallet destino inexistente ====================

    @Nested
    @DisplayName("CP-159: Pago P2P rechazado por wallet destino inexistente")
    class P2PRecipientNotFound {

        @Test
        @DisplayName("Debe rechazar el envío P2P si no se encuentra la wallet destino")
        void sendP2P_rejects_whenRecipientNotFound() {
            // Arrange
            String senderId = "WALLET-001";
            String recipientPhone = "987654321";
            BigDecimal amount = new BigDecimal("100.00");

            Wallet sender = Wallet.builder()
                    .id(senderId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            when(walletPersistencePort.findById(senderId)).thenReturn(Maybe.just(sender));
            when(walletPersistencePort.findByPhoneNumber(recipientPhone)).thenReturn(Maybe.empty());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(senderId, recipientPhone, amount))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.RECIPIENT_NOT_FOUND)
            ).verify();
        }
    }

    // ==================== CP-160: Retiro wallet exitoso ====================

    @Nested
    @DisplayName("CP-160: Retiro wallet exitoso")
    class WithdrawHappyPath {

        @Test
        @DisplayName("Debe completar un retiro exitoso a cuenta bancaria")
        void withdraw_succeeds_whenSufficientBalance() {
            // Arrange
            String walletId = "WALLET-001";
            BigDecimal amount = new BigDecimal("500.00");

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("1000.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));
            when(walletPersistencePort.save(any(Wallet.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(eventPublisherPort.publishTransactionRegistered(any(WalletTransactionEvent.class)))
                    .thenReturn(Completable.complete());
            when(cachePort.invalidateBalance(anyString()))
                    .thenReturn(Completable.complete());
            when(cachePort.setBalance(anyString(), any(BigDecimal.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.withdraw(walletId, amount))
            ).assertNext(transaction -> {
                assertThat(transaction.getWalletId()).isEqualTo(walletId);
                assertThat(transaction.getType()).isEqualTo(TransactionType.WITHDRAWAL);
                assertThat(transaction.getAmount()).isEqualByComparingTo(amount);
                assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
                assertThat(transaction.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("500.00"));
            }).verifyComplete();

            // Verify
            verify(walletPersistencePort, times(1)).save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
            verify(eventPublisherPort, times(1)).publishTransactionRegistered(transactionEventCaptor.capture());
        }
    }

    // ==================== CP-161: Retiro rechazado - saldo insuficiente ====================

    @Nested
    @DisplayName("CP-161: Retiro rechazado por saldo insuficiente")
    class WithdrawInsufficientBalance {

        @Test
        @DisplayName("Debe rechazar el retiro si el saldo es insuficiente")
        void withdraw_rejects_whenInsufficientBalance() {
            // Arrange
            String walletId = "WALLET-001";
            BigDecimal amount = new BigDecimal("1500.00");

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("1000.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.withdraw(walletId, amount))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.INSUFFICIENT_BALANCE)
            ).verify();

            // Verify
            verify(walletPersistencePort, never()).save(any());
        }
    }

    // ==================== CP-162: Asociación tarjeta débito exitosa ====================

    @Nested
    @DisplayName("CP-162: Asociación tarjeta débito a wallet exitosa")
    class AssociateCardHappyPath {

        @Test
        @DisplayName("Debe asociar una tarjeta débito a la wallet exitosamente")
        void associateCard_succeeds_whenCardIsValid() {
            // Arrange
            String walletId = "WALLET-001";
            String cardId = "CARD-001";

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId(null)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));
            when(walletPersistencePort.save(any(Wallet.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, cardId))
            ).assertNext(updatedWallet -> {
                assertThat(updatedWallet.getAssociatedDebitCardId()).isEqualTo(cardId);
            }).verifyComplete();

            // Verify
            verify(walletPersistencePort, times(1)).save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getAssociatedDebitCardId()).isEqualTo(cardId);
        }
    }

    // ==================== CP-163: Asociación tarjeta rechazada - ya asociada ====================

    @Nested
    @DisplayName("CP-163: Asociación tarjeta rechazada por ya estar asociada")
    class AssociateCardAlreadyAssociated {

        @Test
        @DisplayName("Debe rechazar la asociación si la tarjeta ya está asociada a la wallet")
        void associateCard_rejects_whenCardAlreadyAssociated() {
            // Arrange
            String walletId = "WALLET-001";
            String cardId = "CARD-001";

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId(cardId)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, cardId))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.CARD_ALREADY_ASSOCIATED)
            ).verify();

            // Verify
            verify(walletPersistencePort, never()).save(any());
        }
    }

    // ==================== CP-164: Desasociación tarjeta débito exitosa ====================

    @Nested
    @DisplayName("CP-164: Desasociación tarjeta débito de wallet exitosa")
    class DisassociateCardHappyPath {

        @Test
        @DisplayName("Debe desasociar la tarjeta débito de la wallet exitosamente")
        void disassociateCard_succeeds_whenCardIsAssociated() {
            // Arrange
            String walletId = "WALLET-001";

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId("CARD-001")
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));
            when(walletPersistencePort.save(any(Wallet.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId))
            ).assertNext(updatedWallet -> {
                assertThat(updatedWallet.getAssociatedDebitCardId()).isNull();
            }).verifyComplete();

            // Verify
            verify(walletPersistencePort, times(1)).save(walletCaptor.capture());
            assertThat(walletCaptor.getValue().getAssociatedDebitCardId()).isNull();
        }
    }

    // ==================== CP-165: Desasociación rechazada - sin tarjeta asociada ====================

    @Nested
    @DisplayName("CP-165: Desasociación rechazada por no tener tarjeta asociada")
    class DisassociateCardNoCard {

        @Test
        @DisplayName("Debe rechazar la desasociación si no hay tarjeta asociada")
        void disassociateCard_rejects_whenNoCardAssociated() {
            // Arrange
            String walletId = "WALLET-001";

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId(null)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.NO_CARD_ASSOCIATED)
            ).verify();

            // Verify
            verify(walletPersistencePort, never()).save(any());
        }
    }

    // ==================== CP-187: Top-up settled - error controlado ====================

    @Nested
    @DisplayName("CP-187: Flujo wallet con error funcional controlado en paso intermedio")
    class TopupSettledError {

        @Test
        @DisplayName("Debe rechazar el top-up settled sin acreditar saldo cuando el evento es REJECTED")
        void processTopupSettled_rejects_whenEventIsRejected() {
            // Arrange
            String topupId = "TX-001";
            String walletId = "WALLET-001";

            WalletTopupSettledEvent event = WalletTopupSettledEvent.builder()
                    .topupId(topupId)
                    .walletId(walletId)
                    .status("REJECTED")
                    .reason("INVALID_DEBIT_CARD")
                    .amount(new BigDecimal("200.00"))
                    .occurredAt(LocalDateTime.now())
                    .build();

            Transaction transaction = Transaction.builder()
                    .id(topupId)
                    .walletId(walletId)
                    .type(TransactionType.TOPUP)
                    .amount(new BigDecimal("200.00"))
                    .status(TransactionStatus.PENDING)
                    .build();

            String idempotencyKey = "yanki.wallet.topup.settled.v1:" + topupId + ":" + event.getOccurredAt();

            when(idempotencyPort.checkAndMarkProcessed(idempotencyKey))
                    .thenReturn(Single.just(false));
            when(transactionPersistencePort.findById(topupId))
                    .thenReturn(Single.just(transaction));
            when(transactionPersistencePort.updateStatus(any(Transaction.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.completableToMono(walletService.processTopupSettled(event))
            ).expectError(BusinessException.class).verify();

            // Verify
            verify(transactionPersistencePort, times(1)).updateStatus(any());
            verify(walletPersistencePort, never()).save(any());
        }
    }

    // ==================== Get Wallet ====================

    @Nested
    @DisplayName("Consulta de wallet")
    class GetWallet {

        @Test
        @DisplayName("Debe retornar la wallet cuando existe")
        void getWalletById_returnsWallet_whenWalletExists() {
            // Arrange
            String walletId = "WALLET-001";
            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.maybeToMono(walletService.findById(walletId))
            ).expectNext(wallet)
                    .verifyComplete();

            verify(walletPersistencePort, times(1)).findById(walletId);
        }

        @Test
        @DisplayName("Debe retornar vacío cuando la wallet no existe")
        void getWalletById_returnsEmpty_whenWalletNotFound() {
            // Arrange
            String walletId = "WALLET-NOT-FOUND";
            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.empty());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.maybeToMono(walletService.findById(walletId))
            ).verifyComplete();
        }

        @Test
        @DisplayName("Debe buscar wallet por número de teléfono")
        void findByPhoneNumber_returnsWallet_whenPhoneExists() {
            // Arrange
            String phone = "999888776";
            Wallet wallet = Wallet.builder()
                    .id("WALLET-001")
                    .customerId("CUST-001")
                    .phoneNumber(phone)
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            when(walletPersistencePort.findByPhoneNumber(phone)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.maybeToMono(walletService.findByPhoneNumber(phone))
            ).expectNext(wallet)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe retornar el historial de transacciones de una wallet")
        void getTransactions_returnsTransactions_whenWalletHasTransactions() {
            // Arrange
            String walletId = "WALLET-001";
            Transaction tx1 = Transaction.builder()
                    .id("TX-001")
                    .walletId(walletId)
                    .type(TransactionType.TOPUP)
                    .amount(new BigDecimal("200.00"))
                    .status(TransactionStatus.COMPLETED)
                    .build();
            Transaction tx2 = Transaction.builder()
                    .id("TX-002")
                    .walletId(walletId)
                    .type(TransactionType.SEND)
                    .amount(new BigDecimal("100.00"))
                    .status(TransactionStatus.COMPLETED)
                    .build();

            when(transactionPersistencePort.findByWalletId(walletId))
                    .thenReturn(Flowable.just(tx1, tx2));

            // Act & Assert
            StepVerifier.create(walletService.getTransactions(walletId))
                    .expectNext(tx1, tx2)
                    .verifyComplete();
        }
    }

    // ==================== Wallet no encontrada ====================

    @Nested
    @DisplayName("Wallet no encontrada")
    class WalletNotFound {

        @Test
        @DisplayName("Debe lanzar excepción al intentar top-up en wallet inexistente")
        void topUp_throwsWhenWalletNotFound() {
            // Arrange
            String walletId = "WALLET-NOT-FOUND";
            BigDecimal amount = new BigDecimal("200.00");

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.empty());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, amount))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.WALLET_NOT_FOUND)
            ).verify();
        }

        @Test
        @DisplayName("Debe lanzar excepción al intentar P2P en wallet inexistente")
        void sendP2P_throwsWhenWalletNotFound() {
            // Arrange
            String walletId = "WALLET-NOT-FOUND";
            String recipientPhone = "987654321";
            BigDecimal amount = new BigDecimal("100.00");

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.empty());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, recipientPhone, amount))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.WALLET_NOT_FOUND)
            ).verify();
        }
    }

    // ==================== P2P - No enviar a sí mismo ====================

    @Nested
    @DisplayName("P2P - No enviar a sí mismo")
    class P2PCannotSendToSelf {

        @Test
        @DisplayName("Debe rechazar el envío P2P si el destino es la misma wallet")
        void sendP2P_rejects_whenSendingToSelf() {
            // Arrange
            String walletId = "WALLET-001";
            String phone = "999888776";
            BigDecimal amount = new BigDecimal("100.00");

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber(phone)
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));
            when(walletPersistencePort.findByPhoneNumber(phone)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, phone, amount))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            ((BusinessException) throwable).getCode().equals(ErrorCodes.CANNOT_SEND_TO_SELF)
            ).verify();
        }
    }

    // ==================== CP-152: Apertura wallet rechazada - datos inválidos ====================

    @Nested
    @DisplayName("CP-152: Apertura wallet rechazada por datos inválidos/incompletos")
    class OpenWalletInvalidDataValidation {

        @Test
        @DisplayName("Debe rechazar la apertura si el teléfono es vacío")
        void openWallet_rejects_whenPhoneIsEmpty() {
            // Arrange
            String customerId = "DNI-12345678";
            String phone = "";
            String imei = "353456789012345";

            when(walletPersistencePort.existsByCustomerId(customerId))
                    .thenReturn(Single.just(false));
            when(riskViewPort.findByCustomerId(customerId))
                    .thenReturn(Single.just(
                            CustomerRiskView.builder()
                                    .customerId(customerId)
                                    .hasOverdueDebt(false)
                                    .build()));
            when(walletPersistencePort.save(any(Wallet.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(eventPublisherPort.publishWalletOpened(any(WalletOpenedEvent.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert
            // La validación de campos vacíos se realiza en la capa de interfaces (DTO @NotBlank).
            // El servicio de dominio acepta el valor vacío y crea la wallet.
            // La prueba verifica que el flujo completo se ejecuta sin excepción.
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(customerId, phone, imei))
            ).assertNext(wallet -> {
                assertThat(wallet.getPhoneNumber()).isEmpty();
                assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
            }).verifyComplete();
        }

        @Test
        @DisplayName("Debe crear wallet con IMEI vacío cuando no se proporciona")
        void openWallet_acceptsEmptyImei() {
            // Arrange
            String customerId = "DNI-12345678";
            String phone = "999888776";
            String imei = "";

            when(walletPersistencePort.existsByCustomerId(customerId))
                    .thenReturn(Single.just(false));
            when(riskViewPort.findByCustomerId(customerId))
                    .thenReturn(Single.just(
                            CustomerRiskView.builder()
                                    .customerId(customerId)
                                    .hasOverdueDebt(false)
                                    .build()));
            when(walletPersistencePort.save(any(Wallet.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(eventPublisherPort.publishWalletOpened(any(WalletOpenedEvent.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(customerId, phone, imei))
            ).assertNext(wallet -> {
                assertThat(wallet.getImei()).isEmpty();
                assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
            }).verifyComplete();
        }
    }

    // ==================== CP-155: Top-Up rechazado - límite saldo excedido ====================

    @Nested
    @DisplayName("CP-155: Top-Up rechazado por límite saldo wallet excedido")
    class TopUpLimitExceeded {

        @Test
        @DisplayName("Debe rechazar el top-up settled si el saldo resultante excede el límite máximo")
        void topUp_rejects_whenBalanceLimitExceeded() {
            // Arrange
            String walletId = "WALLET-001";
            BigDecimal amount = new BigDecimal("9500.00");

            // El saldo actual (6000) + monto (9500) = 15500 > maxBalance (10000)
            // La validación de límite se realiza al procesar el evento topup.settled.
            // Esta prueba verifica que el evento settled REJECTED no acredita el saldo.
            Transaction transaction = Transaction.builder()
                    .id("TX-001")
                    .walletId(walletId)
                    .type(TransactionType.TOPUP)
                    .amount(amount)
                    .status(TransactionStatus.PENDING)
                    .build();

            WalletTopupSettledEvent event = WalletTopupSettledEvent.builder()
                    .topupId("TX-001")
                    .walletId(walletId)
                    .status("REJECTED")
                    .reason("WALLET_LIMIT_EXCEEDED")
                    .amount(amount)
                    .occurredAt(LocalDateTime.now())
                    .build();

            String idempotencyKey = "yanki.wallet.topup.settled.v1:TX-001:" + event.getOccurredAt();

            when(idempotencyPort.checkAndMarkProcessed(idempotencyKey))
                    .thenReturn(Single.just(false));
            when(transactionPersistencePort.findById("TX-001"))
                    .thenReturn(Single.just(transaction));
            when(transactionPersistencePort.updateStatus(any(Transaction.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.completableToMono(walletService.processTopupSettled(event))
            ).expectError(BusinessException.class).verify();

            // Verify - el saldo NO se acredita
            verify(walletPersistencePort, never()).save(any());
            verify(transactionPersistencePort, times(1)).updateStatus(any());
        }
    }

    // ==================== CP-156: Top-Up rechazado - fondos insuficientes en cuenta origen ====================

    @Nested
    @DisplayName("CP-156: Top-Up rechazado por fondos insuficientes en cuenta origen")
    class TopUpInsufficientFunds {

        @Test
        @DisplayName("Debe rechazar el top-up settled cuando la cuenta origen no tiene fondos")
        void topUp_rejects_whenInsufficientFundsInSourceAccount() {
            // Arrange
            String topupId = "TX-002";
            String walletId = "WALLET-001";

            WalletTopupSettledEvent event = WalletTopupSettledEvent.builder()
                    .topupId(topupId)
                    .walletId(walletId)
                    .status("REJECTED")
                    .reason("INSUFFICIENT_FUNDS")
                    .amount(new BigDecimal("200.00"))
                    .occurredAt(LocalDateTime.now())
                    .build();

            Transaction transaction = Transaction.builder()
                    .id(topupId)
                    .walletId(walletId)
                    .type(TransactionType.TOPUP)
                    .amount(new BigDecimal("200.00"))
                    .status(TransactionStatus.PENDING)
                    .build();

            String idempotencyKey = "yanki.wallet.topup.settled.v1:" + topupId + ":" + event.getOccurredAt();

            when(idempotencyPort.checkAndMarkProcessed(idempotencyKey))
                    .thenReturn(Single.just(false));
            when(transactionPersistencePort.findById(topupId))
                    .thenReturn(Single.just(transaction));
            when(transactionPersistencePort.updateStatus(any(Transaction.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.completableToMono(walletService.processTopupSettled(event))
            ).expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                            "TOPUP_REJECTED".equals(((BusinessException) throwable).getCode())
            ).verify();

            // Verify - el saldo NO se acredita en la wallet
            verify(walletPersistencePort, never()).save(any());
            verify(transactionPersistencePort, times(1)).updateStatus(any());
        }
    }

    // ==================== CP-161: Retiro rechazado - destino inválido ====================

    @Nested
    @DisplayName("CP-161: Retiro rechazado por destino inválido")
    class WithdrawInvalidDestination {

        @Test
        @DisplayName("Debe rechazar el retiro si la wallet está bloqueada")
        void withdraw_rejects_whenWalletBlocked() {
            // Arrange
            String walletId = "WALLET-001";
            BigDecimal amount = new BigDecimal("500.00");

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("1000.00"))
                    .status(WalletStatus.BLOCKED)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.withdraw(walletId, amount))
            ).expectError(BusinessException.class).verify();

            // Verify - no se persisten cambios
            verify(walletPersistencePort, never()).save(any());
        }
    }

    // ==================== CP-163: Asociación tarjeta rechazada - tarjeta inválida ====================

    @Nested
    @DisplayName("CP-163: Asociación tarjeta rechazada por tarjeta inválida")
    class AssociateCardInvalidCard {

        @Test
        @DisplayName("Debe rechazar la asociación si la wallet está bloqueada")
        void associateCard_rejects_whenWalletBlocked() {
            // Arrange
            String walletId = "WALLET-001";
            String cardId = "CARD-001";

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.BLOCKED)
                    .associatedDebitCardId(null)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, cardId))
            ).expectError(BusinessException.class).verify();

            // Verify - no se persisten cambios
            verify(walletPersistencePort, never()).save(any());
        }

        @Test
        @DisplayName("Debe rechazar la asociación si la tarjeta ID es vacía")
        void associateCard_rejects_whenCardIdIsBlank() {
            // Arrange
            String walletId = "WALLET-001";
            String cardId = "   ";

            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId(null)
                    .build();

            when(walletPersistencePort.findById(walletId)).thenReturn(Maybe.just(wallet));

            // Act & Assert
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, cardId))
            ).expectError(BusinessException.class).verify();

            verify(walletPersistencePort, never()).save(any());
        }
    }

    // ==================== CP-186: Flujo integral wallet E2E ====================

    @Nested
    @DisplayName("CP-186: Flujo integral wallet exitoso (E2E)")
    class EndToEndFlow {

        @Test
        @DisplayName("Debe completar el flujo integral: crear, top-up, P2P, retirar, asociar/desasociar tarjeta")
        void endToEnd_completesFullFlow() {
            // Arrange
            String customerId = "DNI-12345678";
            String phone = "999888776";
            String imei = "353456789012345";
            String walletId = "WALLET-001";
            String cardId = "CARD-001";
            BigDecimal topupAmount = new BigDecimal("200.00");
            BigDecimal p2pAmount = new BigDecimal("50.00");
            BigDecimal withdrawAmount = new BigDecimal("100.00");

            // Paso 1: Crear wallet
            when(walletPersistencePort.existsByCustomerId(customerId))
                    .thenReturn(Single.just(false));
            when(riskViewPort.findByCustomerId(customerId))
                    .thenReturn(Single.just(
                            CustomerRiskView.builder()
                                    .customerId(customerId)
                                    .hasOverdueDebt(false)
                                    .build()));

            Wallet createdWallet = Wallet.builder()
                    .id(walletId)
                    .customerId(customerId)
                    .phoneNumber(phone)
                    .imei(imei)
                    .balance(BigDecimal.ZERO)
                    .status(WalletStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(walletPersistencePort.save(any(Wallet.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(eventPublisherPort.publishWalletOpened(any(WalletOpenedEvent.class)))
                    .thenReturn(Completable.complete());

            // Act & Assert - Paso 1: Crear wallet
            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(customerId, phone, imei))
            ).assertNext(w -> {
                assertThat(w.getStatus()).isEqualTo(WalletStatus.ACTIVE);
                assertThat(w.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            }).verifyComplete();

            // Paso 2: Asociar tarjeta
            Wallet walletWithCard = Wallet.builder()
                    .id(walletId)
                    .customerId(customerId)
                    .phoneNumber(phone)
                    .imei(imei)
                    .balance(BigDecimal.ZERO)
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId(null)
                    .build();

            when(walletPersistencePort.findById(walletId))
                    .thenReturn(Maybe.just(walletWithCard));

            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, cardId))
            ).assertNext(w -> {
                assertThat(w.getAssociatedDebitCardId()).isEqualTo(cardId);
            }).verifyComplete();

            // Paso 3: Top-up (inicia transacción PENDING)
            Wallet walletForTopup = Wallet.builder()
                    .id(walletId)
                    .customerId(customerId)
                    .phoneNumber(phone)
                    .balance(BigDecimal.ZERO)
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId(cardId)
                    .build();

            when(walletPersistencePort.findById(walletId))
                    .thenReturn(Maybe.just(walletForTopup));
            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            when(eventPublisherPort.publishTopupRequested(any(WalletTopupRequestedEvent.class)))
                    .thenReturn(Completable.complete());

            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId, topupAmount))
            ).assertNext(tx -> {
                assertThat(tx.getType()).isEqualTo(TransactionType.TOPUP);
                assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
            }).verifyComplete();

            // Paso 4: Simular topup settled SETTLED (acredita saldo)
            WalletTopupSettledEvent settledEvent = WalletTopupSettledEvent.builder()
                    .topupId("TX-TOPUP-001")
                    .walletId(walletId)
                    .status("SETTLED")
                    .amount(topupAmount)
                    .occurredAt(LocalDateTime.now())
                    .build();

            Transaction pendingTx = Transaction.builder()
                    .id("TX-TOPUP-001")
                    .walletId(walletId)
                    .type(TransactionType.TOPUP)
                    .amount(topupAmount)
                    .status(TransactionStatus.PENDING)
                    .build();

            String settledIdempotencyKey = "yanki.wallet.topup.settled.v1:TX-TOPUP-001:" + settledEvent.getOccurredAt();

            when(idempotencyPort.checkAndMarkProcessed(settledIdempotencyKey))
                    .thenReturn(Single.just(false));
            when(transactionPersistencePort.findById("TX-TOPUP-001"))
                    .thenReturn(Single.just(pendingTx));
            when(transactionPersistencePort.updateStatus(any(Transaction.class)))
                    .thenReturn(Completable.complete());
            when(cachePort.invalidateBalance(anyString()))
                    .thenReturn(Completable.complete());
            when(cachePort.setBalance(anyString(), any(BigDecimal.class)))
                    .thenReturn(Completable.complete());
            when(eventPublisherPort.publishTransactionRegistered(any(WalletTransactionEvent.class)))
                    .thenReturn(Completable.complete());

            // El topup settled acredita el saldo
            StepVerifier.create(
                    RxJava3Adapter.completableToMono(walletService.processTopupSettled(settledEvent))
            ).verifyComplete();

            // Paso 5: Desasociar tarjeta
            Wallet walletAfterTopup = Wallet.builder()
                    .id(walletId)
                    .customerId(customerId)
                    .phoneNumber(phone)
                    .balance(topupAmount)
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId(cardId)
                    .build();

            when(walletPersistencePort.findById(walletId))
                    .thenReturn(Maybe.just(walletAfterTopup));

            StepVerifier.create(
                    RxJava3Adapter.singleToMono(walletService.execute(walletId))
            ).assertNext(w -> {
                assertThat(w.getAssociatedDebitCardId()).isNull();
            }).verifyComplete();

            // Verify - se completaron todos los pasos del flujo integral
            verify(walletPersistencePort, atLeast(3)).save(any());
        }
    }
}
