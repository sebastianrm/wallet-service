# Colección Postman — Yanki Wallet Service API

> **Microservicio:** wallet-service (puerto 8087)  
> **Versión de la colección:** Postman v2.1  
> **Casos de prueba cubiertos:** 18 (7 Happy Path + 11 Error)  
> **Fuente:** `test-cases/CASOS_PRUEBA_WALLET.md`

---

## 📋 Descripción

Colección Postman completa para probar la API REST del monedero móvil Yanki. Cubre todos los endpoints del `wallet-service` con documentación detallada, ejemplos de request/response, scripts de validación automatizada y referencias bidireccionales a los casos de prueba (CP), casos de uso (CU) y diagramas de secuencia (SEC).

### Funcionalidades cubiertas

| # | Carpeta | Endpoints | CPs |
|---|---------|-----------|-----|
| 1 | **Apertura de Wallet** | `POST /api/wallets` | CP-150, CP-151, CP-152 |
| 2 | **Consulta de Wallet** | `GET /api/wallets/{id}`, `GET /api/wallets/phone/{phone}` | Consulta por ID y teléfono |
| 3 | **Top-Up (Carga de Saldo)** | `POST /api/wallets/{id}/topup` | CP-153, CP-154 |
| 4 | **Envío P2P** | `POST /api/wallets/{id}/send` | CP-157, CP-158, CP-159 |
| 5 | **Retiro a Cuenta Bancaria** | `POST /api/wallets/{id}/withdraw` | CP-160, CP-161 |
| 6 | **Gestión de Tarjeta Débito** | `POST /api/wallets/{id}/associate-card`, `POST /api/wallets/{id}/disassociate-card` | CP-162, CP-163, CP-164, CP-165 |
| 7 | **Historial de Transacciones** | `GET /api/wallets/{id}/transactions` | Consulta de transacciones |
| 8 | **Flujo Integral E2E** | Secuencia de 7 pasos | CP-186 |

---

## 🚀 Cómo usar

### 1. Importar la colección

1. Abre Postman.
2. Ve a **File → Import** (o presiona `Ctrl+I`).
3. Selecciona el archivo `Yanki-Wallet-Service.postman_collection.json`.
4. La colección aparecerá en el sidebar izquierdo.

### 2. Configurar las variables de colección

La colección incluye 8 variables predefinidas. Edítalas según tu entorno:

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `base_url` | `http://localhost:8087` | URL base del wallet-service |
| `jwt_token` | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` | Token JWT del auth-service |
| `wallet_id` | *(vacío)* | Se llena automáticamente al crear wallet |
| `wallet_id_2` | *(vacío)* | Segunda wallet para pruebas P2P |
| `phone` | `+51999888777` | Teléfono de la wallet principal |
| `phone_2` | `+51988777666` | Teléfono de la wallet destino P2P |
| `debit_card_id` | `CARD-1234567890ABCDEF` | ID de tarjeta débito para top-up |
| `customer_id` | `DNI-70100001` | ID del cliente (documentType-documentNumber) |

**Para editar:** Haz clic en la colección → pestaña **Variables** → modifica los valores.

### 3. Orden de ejecución recomendado

Para probar el flujo completo, ejecuta los requests en este orden:

```
1. Apertura de Wallet
   ├─ CP-150 — Abrir wallet exitosa (Happy Path)
   ├─ CP-151 — Abrir wallet rechazada: duplicidad
   └─ CP-152 — Abrir wallet rechazada: datos inválidos

2. Consulta de Wallet
   ├─ Obtener wallet por ID
   └─ Buscar wallet por teléfono

3. Gestión de Tarjeta Débito
   ├─ CP-162 — Asociar tarjeta débito exitosa
   └─ CP-163 — Asociar tarjeta rechazada: ya asociada

4. Top-Up (Carga de Saldo)
   ├─ CP-153 — Top-Up exitoso (Happy Path)
   └─ CP-154 — Top-Up rechazado: sin tarjeta débito

5. Envío P2P
   ├─ CP-157 — Envío P2P exitoso (Happy Path)
   ├─ CP-158 — P2P rechazado: saldo insuficiente
   └─ CP-159 — P2P rechazado: destino inexistente

6. Retiro a Cuenta Bancaria
   ├─ CP-160 — Retiro exitoso (Happy Path)
   └─ CP-161 — Retiro rechazado: saldo insuficiente

7. Gestión de Tarjeta Débito
   ├─ CP-164 — Desasociar tarjeta débito exitosa
   └─ CP-165 — Desasociar tarjeta rechazada: sin tarjeta asociada

8. Historial de Transacciones
   └─ Obtener historial de transacciones

9. Flujo Integral E2E (CP-186)
   └─ Ejecutar los 7 pasos en secuencia
```

---

## ✅ Scripts de validación automatizada

Cada request incluye scripts de prueba (`pm.test`) que validan automáticamente:

- **Código HTTP esperado** (200, 201, 400, 404, 409)
- **Código de error de negocio** (`WALLET_ALREADY_EXISTS`, `INSUFFICIENT_BALANCE`, etc.)
- **Campos obligatorios en la respuesta** (`id`, `balance`, `status`, etc.)
- **Valores específicos** (balance inicial = 0, status = ACTIVE, type = TOPUP/SEND/WITHDRAWAL)
- **Variables de colección** (guarda automáticamente `wallet_id` tras crear wallet)

Para ver los resultados:
1. Ejecuta un request.
2. Ve a la pestaña **Tests** en el panel de respuesta.
3. Los tests verdes indican éxito; los rojos indican fallo.

---

## 📊 Cobertura de casos de prueba

| CP | Tipo | Endpoint | Método | Descripción |
|----|------|----------|--------|-------------|
| CP-150 | Happy Path | `POST /api/wallets` | Apertura | Wallet creada exitosamente |
| CP-151 | Error | `POST /api/wallets` | Apertura | Duplicidad documento/celular/IMEI |
| CP-152 | Error | `POST /api/wallets` | Apertura | Datos inválidos/incompletos |
| CP-153 | Happy Path | `POST /api/wallets/{id}/topup` | Top-Up | Carga exitosa tripartita |
| CP-154 | Error | `POST /api/wallets/{id}/topup` | Top-Up | Sin tarjeta débito asociada |
| CP-155 | Error | `POST /api/wallets/{id}/topup` | Top-Up | Límite saldo excedido (ejemplo en respuesta) |
| CP-156 | Error | `POST /api/wallets/{id}/topup` | Top-Up | Fondos insuficientes cuenta origen (Kafka) |
| CP-157 | Happy Path | `POST /api/wallets/{id}/send` | P2P | Envío exitoso entre wallets |
| CP-158 | Error | `POST /api/wallets/{id}/send` | P2P | Saldo insuficiente origen |
| CP-159 | Error | `POST /api/wallets/{id}/send` | P2P | Wallet destino inexistente |
| CP-160 | Happy Path | `POST /api/wallets/{id}/withdraw` | Retiro | Retiro exitoso a cuenta |
| CP-161 | Error | `POST /api/wallets/{id}/withdraw` | Retiro | Saldo insuficiente |
| CP-162 | Happy Path | `POST /api/wallets/{id}/associate-card` | Tarjeta | Asociación exitosa |
| CP-163 | Error | `POST /api/wallets/{id}/associate-card` | Tarjeta | Tarjeta ya asociada |
| CP-164 | Happy Path | `POST /api/wallets/{id}/disassociate-card` | Tarjeta | Desasociación exitosa |
| CP-165 | Error | `POST /api/wallets/{id}/disassociate-card` | Tarjeta | Sin tarjeta asociada |
| CP-186 | Happy Path | Múltiples | E2E | Flujo integral completo |
| CP-187 | Error | `POST /api/wallets/{id}/topup` | Top-Up | Error controlado en saga (Kafka) |

**Nota:** CP-155, CP-156 y CP-187 involucran eventos Kafka y validación tripartita. Se incluyen como ejemplos de respuesta en la colección, pero requieren el entorno completo (cards-service + accounts-service + Kafka) para ejecutarse.

---

## 🔐 Autenticación

Todos los endpoints requieren autenticación Bearer JWT. El header `Authorization: Bearer {{jwt_token}}` se incluye automáticamente en cada request.

Para obtener un token:
1. Autenticarse contra el `auth-service` (`POST /api/auth/login`).
2. Copiar el token JWT devuelto.
3. Pegarlo en la variable `jwt_token` de la colección.

---

## 📡 Eventos Kafka

Algunos casos de prueba (CP-153, CP-156, CP-187) involucran eventos Kafka que no se pueden probar directamente con Postman. Para verificar los eventos:

```bash
# Consumir eventos del wallet-service
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic yanki.wallet.transaction.registered.v1 \
  --from-beginning
```

Los eventos publicados por el wallet-service:
- `yanki.wallet.opened.v1` — Apertura exitosa
- `yanki.wallet.topup.requested.v1` — Solicitud de top-up
- `yanki.wallet.transaction.registered.v1` — Transacción registrada (TOPUP/SEND/RECEIVE/WITHDRAWAL)

Los eventos consumidos por el wallet-service:
- `yanki.wallet.topup.settled.v1` — Respuesta de conciliación de top-up
- `yanki.credit.overdue.v1` — Deuda vencida (activa bloqueo)

---

## 🏗️ Respuestas de ejemplo

Cada request incluye respuestas guardadas con los escenarios esperados:

- **200/201 OK** — Respuesta exitosa con el body JSON esperado
- **400 Bad Request** — Error de validación con detalles por campo
- **404 Not Found** — Recurso no encontrado
- **409 Conflict** — Regla de negocio violada

Las respuestas incluyen los headers `Content-Type: application/json` y el formato `ApiError` estandarizado del proyecto.

---

## 🔗 Referencias

| Archivo | Sección | Descripción |
|---------|---------|-------------|
| `../test-cases/CASOS_PRUEBA_WALLET.md` | Todos los CPs | Fuente original de los casos de prueba |
| `../src/main/java/com/yanki/wallet/interfaces/rest/controller/WalletController.java` | 9 endpoints | Implementación REST |
| `../src/main/java/com/yanki/wallet/interfaces/rest/dto/` | DTOs | Esquemas de request/response |
| `../src/main/java/com/yanki/wallet/interfaces/rest/GlobalExceptionHandler.java` | Manejo de errores | Mapeo de códigos de error |
| `../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md` | CU-17, CU-20 | Casos de prueba globales |
| `../../CASOS_PRUEBA/RTM.md` | Fila CU-17, CU-20 | Matriz de trazabilidad |
| `../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md` | Sección 7 | Mapeo wallet-service |
| `../../DIAGRAMAS_SECUENCIAS/SEC015-SEC020.md` | Diagramas | Flujos de secuencia |
| `../README.md` | Documentación del servicio | Endpoints, reglas, modelo |

---

## 📝 Notas

- **Moneda:** Todos los montos están en PEN (Sol peruano).
- **Saldo máximo:** 10,000 PEN (configurable vía `yanki.wallet.max-balance`).
- **Límite diario P2P:** 2,000 PEN (configurable vía `yanki.wallet.p2p.daily-limit`).
- **IMEI:** 15 dígitos numéricos obligatorios para apertura de wallet.
- **Top-up:** Requiere tarjeta débito asociada; el flujo es event-driven tripartito vía Kafka.
- **Bloqueo por deuda vencida:** Si el cliente tiene deuda vencida, el wallet se bloquea automáticamente al consumir `yanki.credit.overdue.v1`.
