# CASOS DE PRUEBA — wallet-service

> **Microservicio:** wallet-service (puerto 8087)  
> **Fuente original:** `CASOS_PRUEBA/CASOS_PRUEBA_QA.md`  
> **Casos de Uso:** CU-17 (Gestión de Wallet), CU-20 (Flujo Integral Wallet)  
> **Diagramas de Secuencia:** SEC015–SEC020, SEC028, SEC034  
> **Total de casos:** 18 (7 Happy Path + 11 Error)

---

## Criterios generales de pruebas

### Precondiciones generales para Happy Path
- Existe un usuario válido con credenciales activas.
- Se dispone de `username` y `password` válidos.
- Los servicios involucrados se encuentran operativos.
- El API Gateway puede validar correctamente el bearer token JWT.
- MongoDB (`yanki_wallet`), Redis y Kafka se encuentran disponibles.
- Cuando aplique, existen datos base previamente cargados: clientes, cuentas, tarjetas o wallets.
- Eureka Server tiene registrados los microservicios backend.

### Criterios de validación comunes
- Validar código HTTP esperado.
- Validar estructura y contenido de la respuesta (JSON schema).
- Validar persistencia en base de datos MongoDB (`yanki_wallet`).
- Validar publicación o consumo de eventos Kafka cuando aplique.
- Validar invalidación/uso de caché Redis cuando aplique.
- Validar mensajes o códigos de error funcional esperados.
- Validar headers de propagación (`X-User-Id`, `X-User-Roles`, `X-Customer-Id`, `X-Request-Id`).
- Validar comportamiento de Circuit Breaker (timeout 2s, fallback) cuando aplique.
- Validar idempotencia cuando aplique (headers `X-Idempotency-Key`).

---

## CU-17: Gestión de Wallet (Yanki Monedero Móvil)

### CP-150 — Apertura de wallet exitosa
- **CU:** CU-17 | **SEC:** SEC015_apertura_wallet.md
- **Tipo**: Happy Path
- **Precondiciones**: Usuario con documento, celular, IMEI, email válidos. NO existe wallet previa. Servicios operativos.
- **Pasos**:
  1. `POST /api/wallets {documentType, documentNumber, phone, imei, email}`.
  2. Validar `201 Created` con `walletId`, `balance=0`, `status=ACTIVE`.
  3. Verificar MongoDB: wallet persistida.
  4. Verificar evento `yanki.wallet.opened.v1`.
- **Postcondiciones**: Wallet creada, saldo 0.

---

### CP-151 — Apertura wallet rechazada: duplicidad (documento/celular/IMEI ya registrado)
- **CU:** CU-17 | **SEC:** SEC015_apertura_wallet.md
- **Tipo**: Error
- **Precondiciones**: Existe wallet con mismo documento O celular O IMEI.
- **Pasos**: Solicitar apertura con datos duplicados.
- **Errores esperados**: HTTP `409` o `422`, código `WALLET_ALREADY_EXISTS`.

---

### CP-152 — Apertura wallet rechazada: datos inválidos/incompletos
- **CU:** CU-17 | **SEC:** SEC015_apertura_wallet.md
- **Tipo**: Error / Validación
- **Precondiciones**: Faltan campos obligatorios.
- **Pasos**: Request incompleto.
- **Errores esperados**: HTTP `422`, código `INVALID_WALLET_DATA`.

---

### CP-153 — Recarga/Top-Up wallet exitosa (Tripartito Wallet→Cards→Accounts→Cards→Wallet)
- **CU:** CU-17, CU-04 | **SEC:** SEC016_topup_wallet.md, SEC034_topup_wallet_tripartito.md
- **Tipo**: Happy Path (Event-Driven 3 servicios, 0 REST interno)
- **Precondiciones**: Wallet ACTIVE, tarjeta débito asociada válida (Cards Service), cuenta vinculada con fondos (Accounts Service).
- **Pasos**:
  1. `POST /api/wallets/{walletId}/topup {amount:200, debitCardId}`.
  2. Validar `202 Accepted` con `requestId`.
  3. Verificar Wallet Service publica `yanki.wallet.topup.requested.v1`.
  4. Verificar Cards Service consume → valida tarjeta débito ACTIVE, cuenta vinculada → publica `yanki.debit.payment.requested.v1`.
  5. Verificar Accounts Service consume → debita cuenta, registra movement `DEBIT_PAYMENT` → publica `debit.payment.settled.v1 SETTLED`.
  6. Verificar Cards Service consume settled → publica `yanki.wallet.topup.settled.v1 SETTLED creditedAmount=200`.
  7. Verificar Wallet Service consume topup.settled → acredita saldo wallet (+200) → publica `yanki.wallet.transaction.registered.v1` type=TOPUP.
  8. Verificar notificación final al usuario (webhook/push).
- **Postcondiciones**: Saldo wallet incrementado, transacción registrada.

---

### CP-154 — Top-Up wallet rechazado: tarjeta débito inválida/inactiva
- **CU:** CU-17 | **SEC:** SEC016_topup_wallet.md
- **Tipo**: Error
- **Precondiciones**: debitCardId no existe, status≠ACTIVE, o no tiene cuenta vinculada.
- **Pasos**: Top-up con tarjeta inválida.
- **Errores esperados**: `topup.settled REJECTED reason=INVALID_DEBIT_CARD`, saldo wallet no cambia.

---

### CP-155 — Top-Up wallet rechazado: límite saldo wallet excedido
- **CU:** CU-17 | **SEC:** SEC016_topup_wallet.md
- **Tipo**: Error / Regla de negocio
- **Precondiciones**: Wallet cerca de límite máximo configurado.
- **Pasos**: Top-up que excede límite.
- **Errores esperados**: HTTP `409` o `topup.settled REJECTED reason=WALLET_LIMIT_EXCEEDED`.

---

### CP-156 — Top-Up wallet rechazado: fondos insuficientes en cuenta origen
- **CU:** CU-17 | **SEC:** SEC016_topup_wallet.md
- **Tipo**: Error
- **Precondiciones**: Cuenta vinculada a tarjeta débito sin saldo.
- **Pasos**: Top-up → `debit.payment.settled REJECTED reason=INSUFFICIENT_FUNDS`.

---

### CP-157 — Pago P2P exitoso entre wallets (por número celular)
- **CU:** CU-17 | **SEC:** SEC017_pago_p2p_wallet.md
- **Tipo**: Happy Path
- **Precondiciones**: Wallet origen saldo ≥ amount, wallet destino existe (buscar por celular).
- **Pasos**:
  1. `POST /api/wallets/{walletId}/payments {phoneDestino, amount:100, description:"Pago cena"}`.
  2. Validar `201` o `202` con débito origen + crédito destino atómico.
  3. Verificar evento `yanki.wallet.transaction.registered.v1` type=P2P_SEND (origen) y P2P_RECEIVE (destino).
  4. Verificar notificaciones a ambos usuarios.
- **Postcondiciones**: Saldos actualizados, transacción registrada.

---

### CP-158 — Pago P2P rechazado: saldo insuficiente origen
- **CU:** CU-17 | **SEC:** SEC017_pago_p2p_wallet.md
- **Tipo**: Error
- **Precondiciones**: Wallet origen saldo < amount.
- **Pasos**: Pago P2P.
- **Errores esperados**: HTTP `409`, código `INSUFFICIENT_WALLET_BALANCE`.

---

### CP-159 — Pago P2P rechazado: wallet destino inexistente (celular no registrado)
- **CU:** CU-17 | **SEC:** SEC017_pago_p2p_wallet.md
- **Tipo**: Error
- **Precondiciones**: Número celular no tiene wallet asociada.
- **Pasos**: Pago P2P.
- **Errores esperados**: HTTP `404`, código `WALLET_DESTINATION_NOT_FOUND`.

---

### CP-160 — Retiro wallet exitoso (a cuenta asociada)
- **CU:** CU-17 | **SEC:** SEC018_retiro_wallet.md
- **Tipo**: Happy Path
- **Precondiciones**: Wallet saldo ≥ amount, cuenta destino válida (asociada o configurada).
- **Pasos**:
  1. `POST /api/wallets/{walletId}/withdrawals {amount:500, destinationAccountId}`.
  2. Validar débito wallet, crédito cuenta.
  3. Verificar evento `wallet.transaction.registered` type=WITHDRAWAL.
- **Postcondiciones**: Saldo wallet reducido.

---

### CP-161 — Retiro wallet rechazado: saldo insuficiente o destino inválido
- **CU:** CU-17 | **SEC:** SEC018_retiro_wallet.md
- **Tipo**: Error
- **Precondiciones**: Saldo < amount O destinationAccountId no válido/no asociada.
- **Pasos**: Retiro inválido.
- **Errores esperados**: HTTP `409` o `404`, códigos `INSUFFICIENT_WALLET_BALANCE` / `INVALID_WITHDRAWAL_DESTINATION`.

---

### CP-162 — Asociación tarjeta débito a wallet exitosa
- **CU:** CU-17 | **SEC:** SEC019_asociacion_tarjeta_wallet.md
- **Tipo**: Happy Path
- **Precondiciones**: Wallet ACTIVE, tarjeta débito válida (consulta Cards Service), no asociada a otra wallet.
- **Pasos**:
  1. `POST /api/wallets/{walletId}/cards {debitCardId}`.
  2. Wallet Service valida en Cards Service: tarjeta existe, type=DEBIT, status=ACTIVE, linkedAccountId válido.
  3. Validar `201` asociación persistida.
  4. Verificar evento asociación publicado.
- **Postcondiciones**: Tarjeta vinculada para top-up.

---

### CP-163 — Asociación tarjeta rechazada: tarjeta inválida/no es débito/ya asociada
- **CU:** CU-17 | **SEC:** SEC019_asociacion_tarjeta_wallet.md
- **Tipo**: Error
- **Precondiciones**: Tarjeta no existe, no es DEBIT, status≠ACTIVE, o ya vinculada a wallet.
- **Pasos**: Asociación inválida.
- **Errores esperados**: HTTP `404`, `409` o `422`.

---

### CP-164 — Desasociación tarjeta débito de wallet exitosa
- **CU:** CU-17 | **SEC:** SEC020_desasociacion_tarjeta_wallet.md
- **Tipo**: Happy Path
- **Precondiciones**: Tarjeta asociada al wallet.
- **Pasos**: `DELETE /api/wallets/{walletId}/cards/{debitCardId}` → `204`, vínculo eliminado, evento desasociación.

---

### CP-165 — Desasociación rechazada: tarjeta no asociada a este wallet
- **CU:** CU-17 | **SEC:** SEC020_desasociacion_tarjeta_wallet.md
- **Tipo**: Error
- **Precondiciones**: Tarjeta no vinculada a este wallet.
- **Pasos**: Desasociar.
- **Errores esperados**: HTTP `404`, código `CARD_NOT_ASSOCIATED`.

---

## CU-20: Flujo Integral Wallet (Consolidado CU-17)

### CP-186 — Flujo integral wallet exitoso: crear → recargar → pagar P2P → retirar → asociar/desasociar tarjeta
- **CU:** CU-20 | **SEC:** SEC015–SEC020
- **Tipo**: Happy Path (End-to-End)
- **Precondiciones**: Usuario autenticado, servicios wallet/cards/accounts operativos.
- **Pasos**:
  1. `POST /api/wallets` → wallet creada.
  2. `POST /api/wallets/{id}/topup` → saldo 200.
  3. `POST /api/wallets/{id}/payments` (P2P) → transferencia a otro wallet.
  4. `POST /api/wallets/{id}/withdrawals` → retiro a cuenta.
  5. `POST /api/wallets/{id}/cards` → asociar tarjeta.
  6. `DELETE /api/wallets/{id}/cards/{cardId}` → desasociar.
- **Validación**: Cada paso `200/201/202`, saldos consistentes, eventos Kafka publicados.

---

### CP-187 — Flujo wallet con error funcional controlado en paso intermedio
- **CU:** CU-20 | **SEC:** SEC016_topup_wallet.md, SEC034_topup_wallet_tripartito.md
- **Tipo**: Error (Transaccionalidad saga)
- **Precondiciones**: Wallet con saldo, intenta top-up con tarjeta inválida.
- **Pasos**: Top-up falla → `topup.settled REJECTED` → wallet NO acredita saldo → estado consistente.
- **Validación**: No hay cambios inconsistentes, compensación automática via eventos.

---

## Resumen de cobertura

| CP | Tipo | Endpoint | Descripción |
|----|------|----------|-------------|
| CP-150 | Happy Path | `POST /api/wallets` | Apertura wallet exitosa |
| CP-151 | Error | `POST /api/wallets` | Duplicidad documento/celular/IMEI |
| CP-152 | Error | `POST /api/wallets` | Datos inválidos/incompletos |
| CP-153 | Happy Path | `POST /api/wallets/{id}/topup` | Top-up tripartito completo |
| CP-154 | Error | `POST /api/wallets/{id}/topup` | Tarjeta débito inválida/inactiva |
| CP-155 | Error | `POST /api/wallets/{id}/topup` | Límite saldo wallet excedido |
| CP-156 | Error | `POST /api/wallets/{id}/topup` | Fondos insuficientes cuenta origen |
| CP-157 | Happy Path | `POST /api/wallets/{id}/payments` | Pago P2P exitoso |
| CP-158 | Error | `POST /api/wallets/{id}/payments` | Saldo insuficiente origen |
| CP-159 | Error | `POST /api/wallets/{id}/payments` | Wallet destino inexistente |
| CP-160 | Happy Path | `POST /api/wallets/{id}/withdrawals` | Retiro exitoso |
| CP-161 | Error | `POST /api/wallets/{id}/withdrawals` | Saldo insuficiente / destino inválido |
| CP-162 | Happy Path | `POST /api/wallets/{id}/cards` | Asociación tarjeta débito exitosa |
| CP-163 | Error | `POST /api/wallets/{id}/cards` | Tarjeta inválida/no débito/ya asociada |
| CP-164 | Happy Path | `DELETE /api/wallets/{id}/cards/{cardId}` | Desasociación exitosa |
| CP-165 | Error | `DELETE /api/wallets/{id}/cards/{cardId}` | Tarjeta no asociada |
| CP-186 | Happy Path | Múltiples | Flujo integral E2E |
| CP-187 | Error | `POST /api/wallets/{id}/topup` | Error controlado en saga |

### Totales
- **Happy Path:** 7 casos (CP-150, CP-153, CP-157, CP-160, CP-162, CP-164, CP-186)
- **Error:** 11 casos (CP-151, CP-152, CP-154, CP-155, CP-156, CP-158, CP-159, CP-161, CP-163, CP-165, CP-187)
- **Total:** 18 casos de prueba

---

## Referencias

| Archivo | Sección | Descripción |
|---------|---------|-------------|
| `../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md` | CU-17, CU-20 | Fuente original de los casos de prueba |
| `../../CASOS_PRUEBA/RTM.md` | Fila CU-17, CU-20 | Matriz de trazabilidad completa |
| `../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md` | Sección 7 | Mapeo wallet-service ↔ CU ↔ SEC ↔ CP |
| `../../DIAGRAMAS_SECUENCIAS/SEC015_apertura_wallet.md` | Diagrama completo | Flujo de apertura wallet |
| `../../DIAGRAMAS_SECUENCIAS/SEC016_topup_wallet.md` | Diagrama completo | Flujo de top-up wallet |
| `../../DIAGRAMAS_SECUENCIAS/SEC017_pago_p2p_wallet.md` | Diagrama completo | Flujo de pago P2P |
| `../../DIAGRAMAS_SECUENCIAS/SEC018_retiro_wallet.md` | Diagrama completo | Flujo de retiro wallet |
| `../../DIAGRAMAS_SECUENCIAS/SEC019_asociacion_tarjeta_wallet.md` | Diagrama completo | Flujo de asociación tarjeta |
| `../../DIAGRAMAS_SECUENCIAS/SEC020_desasociacion_tarjeta_wallet.md` | Diagrama completo | Flujo de desasociación tarjeta |
| `../../DIAGRAMAS_SECUENCIAS/SEC028_job_mora_bloqueo_transversal.md` | Diagrama completo | Bloqueo por deuda vencida |
| `../../DIAGRAMAS_SECUENCIAS/SEC034_topup_wallet_tripartito.md` | Diagrama completo | Top-up tripartito Wallet→Cards→Accounts |
| `../README.md` | Documentación del servicio | Endpoints, reglas de negocio, modelo de datos |
