# Plan de Implementación: Observabilidad, Métricas Micrometer/Prometheus y Hardening de Concurrencia (Java 21 & Spring Boot 3.4)

Fortalecer la API de Orquestación incorporando un ecosistema completo de **Observabilidad 360°** (Logging Estructurado, Trazabilidad Distribuida con MDC y Métricas de Negocio/Sistema con Spring Boot Actuator y Micrometer Prometheus) junto con la **Optimización de Concurrencia CAS y Resiliencia en Compensación** en **Java 21 LTS**.

---

## User Review Required

> [!IMPORTANT]
> **Decisiones Aprobadas en el Review (/grill-me)**
> 1. **Optimización de Concurrencia en `NumeratorClient`**:
>    - Implementación de `generateUniqueIdPair()` para reservar 2 IDs consecutivos (Transacción + Receivable) en **una sola operación atómica CAS** (`PUT /numerator/test-and-set`), reduciendo la contención y el tráfico HTTP en un **50%**.
> 2. **Resiliencia en Rollback Compensatorio**:
>    - Adición de reintentos con Exponential Backoff en `PersistenceClient.deleteTransaction(id)` si falla la conexión con `json-server`.
>    - Emisión de logs de auditoría crítica (`CRITICAL_AUDIT_ALERT`) si la compensación agota sus reintentos para intervención operativa.
> 3. **Métricas Prometheus y Endpoints Actuator**:
>    - Exposición de `/actuator/prometheus` y `/actuator/health` en el puerto `8081`.
>    - Métricas de volumen financiero, comisiones, latencia end-to-end y contadores de conflicto CAS.
> 4. **Prueba de Carga con 200 Hilos Virtuales de Java 21**:
>    - Test de integración de estrés concurrente (`OrchestrationConcurrencyIntegrationTest.java`) ejecutando 200 solicitudes simultáneas mediante `Executors.newVirtualThreadPerTaskExecutor()`.

---

## Proposed Changes

### Archivos de Configuración y Build

#### [MODIFY] [build.gradle](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/build.gradle)
- Agregar `org.springframework.boot:spring-boot-starter-actuator` y `io.micrometer:micrometer-registry-prometheus`.

#### [MODIFY] [application.yml](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/src/main/resources/application.yml)
- Configurar exposición de Actuator (`management.endpoints.web.exposure.include: health,info,metrics,prometheus`).
- Habilitar tags comunes (`application: orchestration-api`).

---

### Componentes de Dominio y Clientes HTTP

#### [MODIFY] [NumeratorClient.java](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/src/main/java/com/tiendanube/orchestration/client/NumeratorClient.java)
- Implementar `generateUniqueIdPair()` reservando `oldValue + 1` (Transacción) y `oldValue + 2` (Receivable) en un único intento CAS.
- Instrumentar métricas `numerator.cas.attempts` y `numerator.cas.conflicts.total` en `MeterRegistry`.

#### [MODIFY] [PersistenceClient.java](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/src/main/java/com/tiendanube/orchestration/client/PersistenceClient.java)
- Agregar reintentos con Exponential Backoff en `deleteTransaction(id)` con logging de auditoría crítica (`CRITICAL_AUDIT_ALERT`).

#### [MODIFY] [OrchestrationService.java](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/src/main/java/com/tiendanube/orchestration/service/OrchestrationService.java)
- Usar `generateUniqueIdPair()` de `NumeratorClient`.
- Inyectar `MeterRegistry` para registrar `transactions.created.total`, `transactions.amount.total`, `orchestration.rollback.total` y `orchestration.creation.latency`.

#### [MODIFY] [TracingFilter.java](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/src/main/java/com/tiendanube/orchestration/filter/TracingFilter.java)
- Logging estructurado de ingreso/egreso HTTP con tiempos de ejecución y MDC.

#### [MODIFY] [GlobalExceptionHandler.java](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/src/main/java/com/tiendanube/orchestration/controller/GlobalExceptionHandler.java)
- Structured WARN/ERROR logging con correlación de `X-Request-Id` y `X-Trace-Id`.

---

### Pruebas de Carga y Concurrencia Masiva

#### [NEW] [OrchestrationConcurrencyIntegrationTest.java](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/src/test/java/com/tiendanube/orchestration/OrchestrationConcurrencyIntegrationTest.java)
- Crear prueba de carga masiva ejecutando **200 peticiones simultáneas** en hilos virtuales (`Executors.newVirtualThreadPerTaskExecutor()`).
- Verificar ausencia de condiciones de carrera, unicidad absoluta de IDs y consistencia en los contadores de Micrometer.

---

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar suite completa con verificación de cobertura JaCoCo $\ge 95\%$:
  `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification`

### Verificación Manual de Métricas
- Probar endpoints Actuator:
  - `curl -i http://localhost:8081/actuator/health`
  - `curl -i http://localhost:8081/actuator/prometheus`
