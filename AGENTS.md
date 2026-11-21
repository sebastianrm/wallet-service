# AGENTS.md — wallet-service

## Agent Identity
- **Name**: `yanki-wallet-agent`
- **Version**: 1.0.0
- **Service**: `wallet-service` (Port 8087)
- **Part**: III (Event-Driven + Wallet Yanki)
- **Status**: 🟨 Esqueleto

---

## Agent Role
**Eres un desarrollador experto full-stack especializado en el stack tecnológico de la plataforma Yanki.**

**Tu expertise incluye:**
- **Java 17 + Spring Boot 3.3.5 + Spring Cloud 2023.0.3** (Config, Eureka, Gateway, CircuitBreaker)
- **Programación reactiva end-to-end**: Project Reactor (WebFlux) + RxJava 3.1.10 (servicios: `Single`/`Maybe`/`Completable`/`Flowable`)
- **Spring Data MongoDB Reactive**: `ReactiveMongoRepository` con métodos derivados, **sin `@Query`**, agregaciones con `ReactiveMongoTemplate`
- **Spring Data Redis Reactive**: Cache distribuido (saldos wallet, teléfono→walletId), TTL configurables
- **Spring Kafka**: Productores/consumidores reactivos, idempotencia (`<topic>:<entityId>:<occurredAt>` en `processed_events`), serialización JSON, 3 topics outbound, 6+ inbound, flujos bidireccionales críticos (top-up wallet, P2P)
- **Spring Cloud Config Client**: Configuración externalizada, fail-fast, retry con backoff
- **Spring Cloud Netflix Eureka Client**: Registro, heartbeat, metadata (zone, version)
- **Resilience4j**: CircuitBreaker (timeout 2s), TimeLimiter, Bulkhead
- **springdoc-openapi 2.6.0**: OpenAPI 3, Swagger UI
- **Checkstyle 10.21.1**: Cero errores (max 160 chars, imports explícitos, naming conventions)
- **JaCoCo 0.8.12**: Cobertura de código
- **Maven 3.9+**: Build, dependency management, plugins

**Principios de desarrollo obligatorios:**
- **Database-per-service**: Acceso exclusivo a `yanki_wallet`; cero acceso a BD de otros servicios
- **Cero configuración en código**: Todo por variables de entorno o Config Server
- **Reactividad pura**: Prohibido `block()`, `subscribe()` en controladores, `Thread.sleep`
- **Nombres en inglés** (clases, métodos, URLs); Javadoc en español
- **Lombok**: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- **Inyección por constructor**: `@RequiredArgsConstructor` en servicios
- **Logging estructurado**: `@Slf4j`, JSON con `traceId`/`spanId` (W3C TraceContext)
- **Endpoints REST resourceful**: `POST/GET/PUT/DELETE /api/<plural>`, operaciones anidadas `POST /api/<plural>/{id}/<action>`
- **Error response unificado**: `ApiError` (`timestamp`, `status`, `error`, `code`, `message`, `path`, `details`)

---

## Role & Responsibilities
Gestiona el **monedero móvil Yanki** (Parte III - funcionalidad nativa event-driven):

1. **Wallet**: Apertura, consulta saldo, cierre
2. **Top-up**: Carga desde tarjeta débito → solicita a cards-service (`yanki.wallet.topup.requested.v1`) → cards liquida → responde (`yanki.wallet.topup.settled.v1`)
3. **P2P**: Envío/recepción entre wallets Yanki (por teléfono)
4. **Retiro**: Extracción a cuenta bancaria (transferencia a accounts)
5. **Asociación débito**: Vincular tarjeta débito para top-ups rápidos
6. **Reglas**:
   - **Bloqueo si deuda vencida** (consume `credit.overdue` → `customer_risk_view`)
   - Un wallet por cliente (customerId único)
   - Top-up solo con tarjeta débito válida (validación con cards)
7. **Vistas locales**:
   - `customer_profile_view` ← `customer.registered/updated` (asociar wallet a cliente bancario)
   - `customer_risk_view` ← `credit.overdue` (bloquear wallet si deuda)
8. **Eventos publicados**:
   - `yanki.wallet.opened.v1`
   - `yanki.wallet.transaction.registered.v1` (TOPUP, SEND, RECEIVE, WITHDRAWAL)
   - `yanki.wallet.topup.requested.v1` (pide a cards)
9. **Eventos consumidos**:
   - `yanki.wallet.topup.settled.v1` (respuesta cards)
   - `yanki.customer.registered/updated/deleted.v1`
   - `yanki.credit.overdue.v1`
   - `yanki.card.opened/closed.v1` (validar tarjeta débito asociada)

---

## Technical Capabilities
| Capability | Implementation |
|------------|----------------|
| **Language** | Java 17, Spring Boot 3.3.5, Spring Cloud 2023.0.3 |
| **Reactive Stack** | Project Reactor (web), RxJava 3.1.10 (servicios: `Single`/`Maybe`/`Completable`/`Flowable`) |
| **Data** | Spring Data MongoDB Reactive (`ReactiveMongoRepository`), **database-per-service** (`yanki_wallet`) |
| **Cache** | Spring Data Redis Reactive (saldos wallet TTL 5m, teléfono→walletId TTL 24h) |
| **Messaging** | Spring Kafka (productor de 3 topics, consumidor de 6+ topics) |
| **Scheduling** | Job conciliación top-ups pendientes, expiración límites P2P |
| **API Docs** | springdoc-openapi 2.6.0 (`/v3/api-docs`, `/swagger-ui.html`) |
| **Resilience** | Resilience4j CircuitBreaker (timeout 2s) |
| **Auth** | JWT HS256 validation (delegada a api-gateway) |
| **Quality** | Checkstyle 10.21.1 (cero errores), JaCoCo 0.8.12 (cobertura) |
| **Build** | Maven 3.9+, `mvn clean verify` |

---

## Communication Protocols

### REST (Inbound)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/wallets` | Abrir wallet (requiere customerId válido) |
| `GET` | `/api/wallets/{id}` | Detalle wallet + saldo |
| `GET` | `/api/wallets/phone/{phone}` | Buscar wallet por teléfono (P2P) |
| `POST` | `/api/wallets/{id}/topup` | Solicitar top-up (debitCardId, amount) |
| `POST` | `/api/wallets/{id}/send` | Enviar dinero (counterpartyPhone, amount) |
| `POST` | `/api/wallets/{id}/withdraw` | Retirar a cuenta bancaria (accountId, amount) |
| `POST` | `/api/wallets/{id}/associate-card` | Asociar tarjeta débito (cardId) |
| `GET` | `/api/wallets/{id}/transactions` | Historial transacciones |

### Kafka (Outbound)
| Topic | Key | Payload | Trigger |
|-------|-----|---------|---------|
| `yanki.wallet.opened.v1` | `walletId` | `WalletOpenedEvent` | Apertura exitosa |
| `yanki.wallet.transaction.registered.v1` | `walletId` | `WalletTransactionRegisteredEvent` | Cada transacción (TOPUP/SEND/RECEIVE/WITHDRAWAL) |
| `yanki.wallet.topup.requested.v1` | `requestId` | `WalletTopUpRequestedEvent` | POST `/topup` |

### Kafka (Inbound)
| Topic | Purpose | Action |
|-------|---------|--------|
| `yanki.wallet.topup.settled.v1` | Respuesta top-up | Confirmar/rechazar carga saldo |
| `yanki.customer.registered.v1` | Nuevo cliente | `customer_profile_view` |
| `yanki.customer.updated.v1` | Cambio perfil/estado | `customer_profile_view` |
| `yanki.customer.deleted.v1` | Cliente eliminado | Bloquear/cerrar wallet |
| `yanki.card.opened.v1` | Tarjeta débito abierta | Validar asociación |
| `yanki.card.closed.v1` | Tarjeta cerrada | Desasociar si era default |
| `yanki.credit.overdue.v1` | Deuda vencida | `customer_risk_view` (bloquear wallet) |

### Local Views
```java
// customer_profile_view
{ customerId, customerType, profile, status, updatedAt }

// customer_risk_view
{ customerId, hasOverdueDebt, overdueAmount, updatedAt }
```

---

## Deployment Configuration

### Docker
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8087` | Puerto HTTP |
| `MONGODB_URI` | `mongodb://mongo:27017/yanki_wallet` | URI MongoDB |
| `REDIS_HOST` | `redis` | Host Redis |
| `REDIS_PORT` | `6379` | Puerto Redis |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092` | Brokers Kafka |
| `KAFKA_GROUP_ID` | `wallet-service` | Consumer group |
| `EUREKA_URL` | `http://discovery-service:8761/eureka/` | Service discovery |
| `CONFIG_SERVER_URL` | `http://config-server:8888` | Config Server |
| `YANKI_WALLET_TOPUP_TIMEOUT_MIN` | `5` | Timeout top-up (min) |
| `YANKI_WALLET_P2P_DAILY_LIMIT` | `2000` | Límite P2P diario (PEN) |
| `YANKI_WALLET_MAX_BALANCE` | `10000` | Saldo máximo wallet (PEN) |

### Health Checks
```yaml
healthcheck:
  test: ["CMD", "wget", "-q", "--spider", "http://localhost:8087/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 45s
```

---

## Observability

### Actuator Endpoints
- `GET /actuator/health` — Liveness/readiness (Mongo, Redis, Kafka)
- `GET /actuator/info` — Versión, build, Git commit
- `GET /actuator/metrics` — Métricas JVM, HTTP, Kafka, Redis
- `GET /actuator/prometheus` — Exportador Prometheus

### Key Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `wallets.opened` | Counter | Wallets abiertos |
| `wallets.topup.requested` | Counter | Top-ups solicitados |
| `wallets.topup.settled` | Counter | Por estado (SETTLED/REJECTED) |
| `wallets.p2p.sent` | Counter | Envíos P2P |
| `wallets.p2p.received` | Counter | Recepciones P2P |
| `wallets.withdrawals` | Counter | Retiros a cuenta |
| `wallets.blocked.risk` | Counter | Bloqueos por risk view |

### Logging
- **Framework**: Logback (SLF4J)
- **Level**: `INFO` negocio, `DEBUG` trazas, `ERROR` fallos
- **Format**: JSON estructurado con `traceId`, `spanId` (W3C TraceContext)
- **Correlation**: `X-Request-ID` header propagado en toda la cadena

### Distributed Tracing
- **Propagation**: `traceparent` / `tracestate` headers
- **Spans**: Top-up (bidireccional cards), P2P, retiro, asociación tarjeta
- **Trace**: `walletId`, `requestId` propagados en Kafka y REST

---

## Agent Lifecycle

### Startup Sequence
1. Config Server → Eureka → MongoDB (índices: customerId único, phone único, walletId)
2. Redis (saldos, phone→walletId mapping) → Kafka (consumer group, 6+ subscriptions)
3. **Reconstruir vistas locales** (profile, risk)
4. Iniciar **jobs**: conciliación top-ups (cada 5 min), expiración límites P2P (diario medianoche)
5. HTTP server 8087

### Shutdown
1. Deregister Eureka → Detener jobs → Flush Kafka → Cerrar conexiones

---

## Quality Gates (Pre-deploy)
```bash
mvn clean verify
# Debe pasar:
# - Checkstyle: 0 errores
# - Tests: 100% verdes (JUnit 5 + Mockito + StepVerifier)
# - JaCoCo: reporte generado en target/site/jacoco/index.html
# - Compilación: BUILD SUCCESS
```

---

## Security
- JWT en gateway
- Roles: ADMIN, OPERATOR, WALLET_USER (cliente final)
- **P2P**: Validación teléfono destino existe (Redis lookup)
- **Top-up**: Solo tarjetas débito asociadas y validadas por cards-service
- Auditoría: Todas las transacciones → eventos Kafka

---

## Error Handling
| Code | HTTP | Cause |
|------|------|-------|
| `WALLET_NOT_FOUND` | 404 | Wallet no existe |
| `WALLET_ALREADY_EXISTS` | 409 | Cliente ya tiene wallet |
| `INSUFFICIENT_BALANCE` | 400 | Saldo insuficiente para send/withdraw |
| `COUNTERPARTY_NOT_FOUND` | 404 | Teléfono destino sin wallet |
| `DAILY_P2P_LIMIT_EXCEEDED` | 400 | Límite P2P diario superado |
| `MAX_BALANCE_EXCEEDED` | 400 | Top-up excede saldo máximo |
| `WALLET_BLOCKED_RISK` | 403 | Deuda vencida (risk view) |
| `DEBIT_CARD_NOT_ASSOCIATED` | 400 | Tarjeta no asociada o cerrada |
| `TOPUP_TIMEOUT` | 408 | Cards no respondió en timeout |

---

## Testing Strategy
- **Unit**: Servicios de dominio (RxJava `Single`/`Flowable` + `StepVerifier`)
- **Integration**: Controladores REST (`WebTestClient`), repositorios (`@DataMongoTest`)
- **Event**: EmbeddedKafka (top-up bidireccional cards, risk view updates)
- **Contract**: Consumer-driven contracts con Pact (Kafka topics)
- **Test Cases**: Ver `test-cases/CASOS_PRUEBA_WALLET.md` (18 casos: 7 HP + 11 Error)

---

## Runbook

### Escalar
```bash
docker compose -f docker-compose.full.yml up -d --scale wallet-service=3
# Consumer group idéntico, Redis compartido (saldos, phone mapping, límites P2P)
```

### Reconstruir vistas
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group wallet-service --reset-offsets --to-earliest --all-topics --execute
```

### Conciliación top-ups manual
```bash
curl -X POST http://localhost:8087/actuator/jobs/topup-conciliation/run
```

### Ver wallets bloqueados por riesgo
```bash
curl -s http://localhost:8087/actuator/metrics/wallets.blocked.risk
```

### Reset límites P2P (emergencia)
```bash
redis-cli -h localhost -p 6379 KEYS "yanki:wallet:p2p:*" | xargs redis-cli DEL
```

---

## Dependencias
| Agent | Relationship | Protocol |
|-------|--------------|----------|
| `yanki-customers-agent` | Consume cliente events | Kafka |
| `yanki-cards-agent` | Bidireccional: wallet.topup (requested/settled), card.opened/closed | Kafka |
| `yanki-credits-agent` | Consume overdue (bloqueo wallet) | Kafka |
| `yanki-reports-agent` | Produce wallet events | Kafka |
| `yanki-api-gateway-agent` | Ruta `/api/wallets/**` | REST |
| `yanki-discovery-agent` | Registro | Eureka |
| `yanki-config-agent` | Configuración | Spring Cloud Config |

---

## Part III Specifics (Nativo Event-Driven)
- **No REST calls to other services** — Solo Kafka (top-up ↔ cards, riesgo ← credits)
- **P2P interno** — No sale del servicio (wallet a wallet)
- **Vistas locales críticas** — `risk_view` bloquea TODO (topup, send, withdraw)
- **Idempotencia** — `<topic>:<entityId>:<occurredAt>` en `processed_events`
- **Phone mapping** — Redis `yanki:wallet:phone:<phone>` → `walletId` (TTL 24h, refresh on use)

---

## Versioning & Compatibility
- **API Version**: v1 (path `/api/...`)
- **Event Version**: v1 (topic suffix `.v1`)
- **Breaking Changes**: Nuevo topic `.v2`, mantener `.v1` 6 meses
- **Deprecation**: Header `Sunset` en respuestas REST, log WARNING en consumidores Kafka

---

## Contact
- **Team**: Platform - Wallet Yanki
- **Slack**: `#yanki-wallet`
- **Repo**: `github.com/yanki/wallet-service` (monorepo: `yanki-accounts-service/wallet-service`)
- **On-call**: Rotación semanal (ver OpsGenie)