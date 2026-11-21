package com.yanki.wallet.domain.model;

import com.yanki.wallet.domain.exception.BusinessException;
import com.yanki.wallet.domain.exception.ErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias del modelo de dominio Wallet.
 *
 * <p>Verifica el comportamiento de las entidades puras de dominio
 * sin dependencias externas.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@DisplayName("Wallet - Modelo de dominio")
class WalletModelTest {

    private Wallet createActiveWallet() {
        return Wallet.builder()
                .id("WALLET-001")
                .customerId("CUST-001")
                .phoneNumber("999888776")
                .balance(new BigDecimal("500.00"))
                .status(WalletStatus.ACTIVE)
                .associatedDebitCardId(null)
                .build();
    }

    private Wallet createBlockedWallet() {
        return Wallet.builder()
                .id("WALLET-001")
                .customerId("CUST-001")
                .phoneNumber("999888776")
                .balance(new BigDecimal("500.00"))
                .status(WalletStatus.BLOCKED)
                .associatedDebitCardId(null)
                .build();
    }

    // ==================== Top-Up ====================

    @Nested
    @DisplayName("Top-Up (Carga de saldo)")
    class TopUpTests {

        @Test
        @DisplayName("Debe aumentar el saldo al realizar top-up")
        void topUp_increasesBalance() {
            // Arrange
            Wallet wallet = createActiveWallet();
            BigDecimal amount = new BigDecimal("200.00");

            // Act
            Wallet updated = wallet.topUp(amount);

            // Assert
            assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
            assertThat(updated.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        }

        @Test
        @DisplayName("Debe rechazar top-up si la wallet está bloqueada")
        void topUp_throwsWhenWalletBlocked() {
            // Arrange
            Wallet wallet = createBlockedWallet();
            BigDecimal amount = new BigDecimal("200.00");

            // Act & Assert
            assertThatThrownBy(() -> wallet.topUp(amount))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no se encuentra activa");
        }

        @Test
        @DisplayName("Debe rechazar top-up con monto cero")
        void topUp_throwsWhenAmountIsZero() {
            // Arrange
            Wallet wallet = createActiveWallet();
            BigDecimal amount = BigDecimal.ZERO;

            // Act & Assert
            assertThatThrownBy(() -> wallet.topUp(amount))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getCode())
                    .isEqualTo(ErrorCodes.INVALID_AMOUNT);
        }

        @Test
        @DisplayName("Debe rechazar top-up con monto negativo")
        void topUp_throwsWhenAmountIsNegative() {
            // Arrange
            Wallet wallet = createActiveWallet();
            BigDecimal amount = new BigDecimal("-100.00");

            // Act & Assert
            assertThatThrownBy(() -> wallet.topUp(amount))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getCode())
                    .isEqualTo(ErrorCodes.INVALID_AMOUNT);
        }

        @Test
        @DisplayName("Debe rechazar top-up con monto nulo")
        void topUp_throwsWhenAmountIsNull() {
            // Arrange
            Wallet wallet = createActiveWallet();

            // Act & Assert
            assertThatThrownBy(() -> wallet.topUp(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getCode())
                    .isEqualTo(ErrorCodes.INVALID_AMOUNT);
        }
    }

    // ==================== Debit (Débito) ====================

    @Nested
    @DisplayName("Debit (Débito de saldo)")
    class DebitTests {

        @Test
        @DisplayName("Debe reducir el saldo al realizar débito")
        void debit_decreasesBalance() {
            // Arrange
            Wallet wallet = createActiveWallet();
            BigDecimal amount = new BigDecimal("200.00");

            // Act
            Wallet updated = wallet.debit(amount);

            // Assert
            assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(updated.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        }

        @Test
        @DisplayName("Debe rechazar débito si el saldo es insuficiente")
        void debit_throwsWhenInsufficientBalance() {
            // Arrange
            Wallet wallet = createActiveWallet();
            BigDecimal amount = new BigDecimal("600.00");

            // Act & Assert
            assertThatThrownBy(() -> wallet.debit(amount))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getCode())
                    .isEqualTo(ErrorCodes.INSUFFICIENT_BALANCE);
        }

        @Test
        @DisplayName("Debe rechazar débito si la wallet está bloqueada")
        void debit_throwsWhenWalletBlocked() {
            // Arrange
            Wallet wallet = createBlockedWallet();
            BigDecimal amount = new BigDecimal("100.00");

            // Act & Assert
            assertThatThrownBy(() -> wallet.debit(amount))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no se encuentra activa");
        }

        @Test
        @DisplayName("Debe permitir débito exacto al saldo disponible")
        void debit_allowsExactBalanceDebit() {
            // Arrange
            Wallet wallet = createActiveWallet();
            BigDecimal amount = new BigDecimal("500.00");

            // Act
            Wallet updated = wallet.debit(amount);

            // Assert
            assertThat(updated.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ==================== Associate Card ====================

    @Nested
    @DisplayName("Associate Card (Asociar tarjeta)")
    class AssociateCardTests {

        @Test
        @DisplayName("Debe asociar una tarjeta débito a la wallet")
        void associateCard_setsCardId() {
            // Arrange
            Wallet wallet = createActiveWallet();
            String cardId = "CARD-001";

            // Act
            Wallet updated = wallet.associateCard(cardId);

            // Assert
            assertThat(updated.getAssociatedDebitCardId()).isEqualTo(cardId);
        }

        @Test
        @DisplayName("Debe rechazar asociación si la wallet está bloqueada")
        void associateCard_throwsWhenWalletBlocked() {
            // Arrange
            Wallet wallet = createBlockedWallet();
            String cardId = "CARD-001";

            // Act & Assert
            assertThatThrownBy(() -> wallet.associateCard(cardId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no se encuentra activa");
        }

        @Test
        @DisplayName("Debe rechazar asociación con tarjeta nula")
        void associateCard_throwsWhenCardIdIsNull() {
            // Arrange
            Wallet wallet = createActiveWallet();

            // Act & Assert
            assertThatThrownBy(() -> wallet.associateCard(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getCode())
                    .isEqualTo(ErrorCodes.VALIDATION_ERROR);
        }

        @Test
        @DisplayName("Debe rechazar asociación con tarjeta vacía")
        void associateCard_throwsWhenCardIdIsBlank() {
            // Arrange
            Wallet wallet = createActiveWallet();

            // Act & Assert
            assertThatThrownBy(() -> wallet.associateCard("   "))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getCode())
                    .isEqualTo(ErrorCodes.VALIDATION_ERROR);
        }
    }

    // ==================== Disassociate Card ====================

    @Nested
    @DisplayName("Disassociate Card (Desasociar tarjeta)")
    class DisassociateCardTests {

        @Test
        @DisplayName("Debe desasociar la tarjeta débito de la wallet")
        void disassociateCard_removesCardId() {
            // Arrange
            Wallet wallet = Wallet.builder()
                    .id("WALLET-001")
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.ACTIVE)
                    .associatedDebitCardId("CARD-001")
                    .build();

            // Act
            Wallet updated = wallet.disassociateCard();

            // Assert
            assertThat(updated.getAssociatedDebitCardId()).isNull();
        }

        @Test
        @DisplayName("Debe rechazar desasociación si la wallet está bloqueada")
        void disassociateCard_throwsWhenWalletBlocked() {
            // Arrange
            Wallet wallet = Wallet.builder()
                    .id("WALLET-001")
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(new BigDecimal("500.00"))
                    .status(WalletStatus.BLOCKED)
                    .associatedDebitCardId("CARD-001")
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> wallet.disassociateCard())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no se encuentra activa");
        }
    }

    // ==================== Block / Unblock ====================

    @Nested
    @DisplayName("Block / Unblock (Bloqueo y desbloqueo)")
    class BlockUnblockTests {

        @Test
        @DisplayName("Debe bloquear una wallet activa")
        void block_changesStatusToBlocked() {
            // Arrange
            Wallet wallet = createActiveWallet();

            // Act
            Wallet updated = wallet.block();

            // Assert
            assertThat(updated.getStatus()).isEqualTo(WalletStatus.BLOCKED);
        }

        @Test
        @DisplayName("Debe desbloquear una wallet bloqueada")
        void unblock_changesStatusToActive() {
            // Arrange
            Wallet wallet = createBlockedWallet();

            // Act
            Wallet updated = wallet.unblock();

            // Assert
            assertThat(updated.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        }
    }

    // ==================== Close ====================

    @Nested
    @DisplayName("Close (Cerrar wallet)")
    class CloseTests {

        @Test
        @DisplayName("Debe cerrar una wallet con saldo cero")
        void close_succeedsWhenBalanceIsZero() {
            // Arrange
            Wallet wallet = Wallet.builder()
                    .id("WALLET-001")
                    .customerId("CUST-001")
                    .phoneNumber("999888776")
                    .balance(BigDecimal.ZERO)
                    .status(WalletStatus.ACTIVE)
                    .build();

            // Act
            Wallet updated = wallet.close();

            // Assert
            assertThat(updated.getStatus()).isEqualTo(WalletStatus.CLOSED);
        }

        @Test
        @DisplayName("Debe rechazar cierre si el saldo no es cero")
        void close_throwsWhenBalanceIsNotZero() {
            // Arrange
            Wallet wallet = createActiveWallet();

            // Act & Assert
            assertThatThrownBy(() -> wallet.close())
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getCode())
                    .isEqualTo(ErrorCodes.VALIDATION_ERROR);
        }
    }

    // ==================== isActive ====================

    @Nested
    @DisplayName("isActive (Verificar estado activo)")
    class IsActiveTests {

        @Test
        @DisplayName("Debe retornar true si la wallet está activa")
        void isActive_returnsTrueWhenActive() {
            // Arrange
            Wallet wallet = createActiveWallet();

            // Act & Assert
            assertThat(wallet.isActive()).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false si la wallet está bloqueada")
        void isActive_returnsFalseWhenBlocked() {
            // Arrange
            Wallet wallet = createBlockedWallet();

            // Act & Assert
            assertThat(wallet.isActive()).isFalse();
        }
    }
}
