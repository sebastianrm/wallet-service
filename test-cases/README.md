# Test Cases — wallet-service

Directorio de casos de prueba del microservicio **wallet-service** (monedero móvil Yanki, puerto 8087).

## Archivos

| Archivo | Descripción |
|---------|-------------|
| [CASOS_PRUEBA_WALLET.md](CASOS_PRUEBA_WALLET.md) | 18 casos de prueba detallados (CU-17 + CU-20) |

## Resumen

| Caso de Uso | Nombre | Casos de Prueba | Happy Path | Error |
|-------------|--------|-----------------|------------|-------|
| **CU-17** | Gestión de Wallet | CP-150 a CP-165 | 6 | 10 |
| **CU-20** | Flujo Integral Wallet | CP-186, CP-187 | 1 | 1 |
| **Total** | — | **18 casos** | **7** | **11** |

## Cobertura por funcionalidad

| Funcionalidad | Endpoint | Casos |
|--------------|----------|-------|
| **Apertura wallet** | `POST /api/wallets` | CP-150, CP-151, CP-152 |
| **Top-Up (recarga)** | `POST /api/wallets/{id}/topup` | CP-153, CP-154, CP-155, CP-156 |
| **Pago P2P** | `POST /api/wallets/{id}/payments` | CP-157, CP-158, CP-159 |
| **Retiro a cuenta** | `POST /api/wallets/{id}/withdrawals` | CP-160, CP-161 |
| **Asociar tarjeta débito** | `POST /api/wallets/{id}/cards` | CP-162, CP-163 |
| **Desasociar tarjeta débito** | `DELETE /api/wallets/{id}/cards/{cardId}` | CP-164, CP-165 |
| **Flujo integral E2E** | Múltiples | CP-186, CP-187 |

## Diagramas de Secuencia relacionados

| Diagrama | Flujo |
|----------|-------|
| SEC015 | Apertura wallet |
| SEC016 | Top-up wallet |
| SEC017 | Pago P2P entre wallets |
| SEC018 | Retiro wallet a cuenta |
| SEC019 | Asociación tarjeta débito |
| SEC020 | Desasociación tarjeta débito |
| SEC028 | Bloqueo por deuda vencida (transversal) |
| SEC034 | Top-up tripartito (Wallet→Cards→Accounts) |

## Referencias

- **Fuente original:** `../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md`
- **Matriz de trazabilidad:** `../../CASOS_PRUEBA/RTM.md`
- **Trazabilidad por servicio:** `../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md` (Sección 7)
- **Documentación del servicio:** `../README.md`
