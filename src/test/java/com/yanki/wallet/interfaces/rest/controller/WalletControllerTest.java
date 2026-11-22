package com.yanki.wallet.interfaces.rest.controller;

import com.yanki.wallet.domain.exception.BusinessException;
import com.yanki.wallet.domain.exception.ErrorCodes;
import com.yanki.wallet.domain.model.Wallet;
import com.yanki.wallet.domain.model.WalletStatus;
import com.yanki.wallet.domain.port.in.AssociateCardUseCase;
import com.yanki.wallet.domain.port.in.DisassociateCardUseCase;
import com.yanki.wallet.domain.port.in.GetWalletUseCase;
import com.yanki.wallet.domain.port.in.OpenWalletUseCase;
import com.yanki.wallet.domain.port.in.SendP2PUseCase;
import com.yanki.wallet.domain.port.in.TopUpWalletUseCase;
import com.yanki.wallet.domain.port.in.WithdrawWalletUseCase;
import com.yanki.wallet.interfaces.rest.dto.WalletResponse;
import com.yanki.wallet.interfaces.rest.mapper.WalletRestMapper;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del controlador REST WalletController.
 *
 * <p>Valida que los endpoints retornen los códigos HTTP correctos,
 * especialmente el caso 2.4: wallet inexistente debe retornar 404
 * (BusinessException con WALLET_NOT_FOUND) y no 204 No Content ni 500.</p>
 *
 * <p>El test verifica que el flujo reactivo del controller convierte
 * Maybe.empty() en BusinessException vía switchIfEmpty, lo cual permite
 * que GlobalExceptionHandler retorne HTTP 404 correctamente.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private GetWalletUseCase getWalletUseCase;

    @Mock
    private OpenWalletUseCase openWalletUseCase;

    @Mock
    private TopUpWalletUseCase topUpWalletUseCase;

    @Mock
    private SendP2PUseCase sendP2PUseCase;

    @Mock
    private WithdrawWalletUseCase withdrawWalletUseCase;

    @Mock
    private AssociateCardUseCase associateCardUseCase;

    @Mock
    private DisassociateCardUseCase disassociateCardUseCase;

    private WalletController walletController;

    @BeforeEach
    void setUp() {
        walletController = new WalletController(
                openWalletUseCase,
                topUpWalletUseCase,
                sendP2PUseCase,
                withdrawWalletUseCase,
                associateCardUseCase,
                disassociateCardUseCase,
                getWalletUseCase
        );
    }

    // ==================== Issue 2.4: Wallet inexistente retorna 404 ====================

    @Nested
    @DisplayName("Issue 2.4: GET /api/wallets/{id} - Wallet no encontrada")
    class GetWalletByIdNotFound {

        @Test
        @DisplayName("Debe lanzar BusinessException WALLET_NOT_FOUND cuando la wallet no existe (no retornar Mono.empty/204)")
        void getWalletById_throwsWalletNotFound_whenWalletDoesNotExist() {
            // Arrange: findById retorna Maybe.empty() cuando la wallet no existe
            String nonExistentId = "WALLET-NOT-FOUND";
            when(getWalletUseCase.findById(nonExistentId)).thenReturn(Maybe.empty());

            // Act: El controller debe convertir Maybe.empty() en BusinessException
            Mono<WalletResponse> result = walletController.getWalletById(nonExistentId);

            // Assert: debe lanzar BusinessException con WALLET_NOT_FOUND (que GlobalExceptionHandler mapea a 404)
            // NO debe retornar Mono.empty() que Spring traduciría a 204 No Content
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals(ErrorCodes.WALLET_NOT_FOUND)
                    ).verify();
        }

        @Test
        @DisplayName("Debe retornar WalletResponse cuando la wallet existe")
        void getWalletById_returnsWalletResponse_whenWalletExists() {
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
            when(getWalletUseCase.findById(walletId)).thenReturn(Maybe.just(wallet));

            // Act
            Mono<WalletResponse> result = walletController.getWalletById(walletId);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.id()).isEqualTo(walletId);
                        assertThat(response.customerId()).isEqualTo("CUST-001");
                        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("500.00"));
                        assertThat(response.status()).isEqualTo("ACTIVE");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Issue 2.4: GET /api/wallets/phone/{phone} - Wallet no encontrada por teléfono")
    class GetWalletByPhoneNotFound {

        @Test
        @DisplayName("Debe lanzar BusinessException WALLET_NOT_FOUND cuando no existe wallet para el teléfono")
        void getWalletByPhone_throwsWalletNotFound_whenPhoneNotFound() {
            // Arrange
            String phone = "+51999000111";
            when(getWalletUseCase.findByPhoneNumber(phone)).thenReturn(Maybe.empty());

            // Act
            Mono<WalletResponse> result = walletController.getWalletByPhone(phone);

            // Assert
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals(ErrorCodes.WALLET_NOT_FOUND)
                    ).verify();
        }

        @Test
        @DisplayName("Debe retornar WalletResponse cuando existe wallet para el teléfono")
        void getWalletByPhone_returnsWalletResponse_whenPhoneExists() {
            // Arrange
            String phone = "+51999888777";
            Wallet wallet = Wallet.builder()
                    .id("WALLET-001")
                    .customerId("CUST-001")
                    .phoneNumber(phone)
                    .balance(new BigDecimal("300.00"))
                    .status(WalletStatus.ACTIVE)
                    .build();
            when(getWalletUseCase.findByPhoneNumber(phone)).thenReturn(Maybe.just(wallet));

            // Act
            Mono<WalletResponse> result = walletController.getWalletByPhone(phone);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.phoneNumber()).isEqualTo(phone);
                    })
                    .verifyComplete();
        }
    }

    // ==================== Verificación del fix: sin switchIfEmpty, Maybe.empty() produce Mono.empty ====================

    @Nested
    @DisplayName("Verificación del fix: comportamiento sin switchIfEmpty")
    class VerifySwitchIfEmptyFix {

        @Test
        @DisplayName("Sin switchIfEmpty, maybeToMono con Maybe.empty() produce Mono.empty() que Spring traduce a 204")
        void withoutSwitchIfEmpty_maybeToMonoProducesEmptyMono() {
            // Arrange: simula el comportamiento ANTES del fix
            String nonExistentId = "WALLET-NOT-FOUND";
            when(getWalletUseCase.findById(nonExistentId)).thenReturn(Maybe.empty());

            // Act: sin switchIfEmpty, maybeToMono retorna Mono.empty()
            Mono<WalletResponse> resultWithoutFix = RxJava3Adapter.maybeToMono(
                    getWalletUseCase.findById(nonExistentId)
                            .map(WalletRestMapper::toResponse)
            );

            // Assert: Mono.empty() se completa sin emitir elementos → Spring lo traduce a 204 No Content
            // Este test demuestra el BUG original: 204 en lugar de 404
            StepVerifier.create(resultWithoutFix)
                    .verifyComplete(); // Se completa sin error → 204 No Content (BUG)
        }

        @Test
        @DisplayName("Con switchIfEmpty, maybeToMono con Maybe.empty() produce BusinessException → 404")
        void withSwitchIfEmpty_maybeToMonoProducesBusinessException() {
            // Arrange: simula el comportamiento DESPUÉS del fix
            String nonExistentId = "WALLET-NOT-FOUND";
            when(getWalletUseCase.findById(nonExistentId)).thenReturn(Maybe.empty());

            // Act: con switchIfEmpty, Maybe.empty() se convierte en BusinessException
            Mono<WalletResponse> resultWithFix = RxJava3Adapter.maybeToMono(
                    getWalletUseCase.findById(nonExistentId)
                            .map(WalletRestMapper::toResponse)
            ).switchIfEmpty(Mono.error(new BusinessException(
                    ErrorCodes.WALLET_NOT_FOUND,
                    "Wallet no encontrada: " + nonExistentId)));

            // Assert: BusinessException con WALLET_NOT_FOUND → GlobalExceptionHandler retorna 404
            StepVerifier.create(resultWithFix)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals(ErrorCodes.WALLET_NOT_FOUND)
                    ).verify();
        }
    }
}
