# Plan de Implementación: API de Orquestación (Java 21 LTS y Spring Boot 3.4)

Construir una API de Orquestación de nivel empresarial en **Java 21 LTS** utilizando **Spring Boot 3.4**, **Gradle** y prácticas de arquitectura limpia (KISS, SOLID, DRY). La API orquestará la creación de transacciones, el cálculo de comisiones y receivables, y la asignación de IDs únicos mediante `numerator-api`, garantizando consistencia de datos, alta concurrencia, observabilidad y documentación interactiva.

---

## Novedades y Características de Java 21 LTS Aprobadas

> [!IMPORTANT]
> **Aprovechamiento Tecnológico de Java 21 LTS**
> 1. **Trazabilidad Distribuida con SLF4J MDC**:
>    - Uso de MDC de SLF4J en `TracingFilter` para compartir identificadores de trazabilidad (`X-Request-Id`, `X-Trace-Id`) de manera inmutable y de alto rendimiento a través de Virtual Threads.
> 2. **Inmutabilidad y Clases Record**:
>    - DTOs y Value Objects inmutables nativos con validaciones de dominio.
> 3. **Sequenced Collections (JEP 431)**:
>    - Uso estricto de `.getFirst()` en lugar de `.get(0)` para un acceso expresivo y seguro en colecciones ordenadas.
> 4. **Virtual Threads (Project Loom)**:
>    - Activados nativamente (`spring.threads.virtual.enabled=true`) para el manejo masivo de peticiones concurrentes sin saturación de I/O.

---

## Documentación API, README y Postman Collection

> [!IMPORTANT]
> **1. README Completo y Diagramas de Flujo Mermaid**
> - Creación del archivo [`orchestration-api/README.md`](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/README.md) detallando stack tecnológico, reglas de negocio, guía de ejecución con Docker Compose y diagrama de secuencia Mermaid.

> [!IMPORTANT]
> **2. Colección Oficial de Postman Reubicada**
> - Reubicación de [`postman_collection.json`](file:///Users/eze/projects/merchant-transactions-api-regqmm/orchestration-api/postman_collection.json) en la raíz del proyecto `orchestration-api/` junto al `README.md`.

> [!IMPORTANT]
> **3. OpenAPI 3.0 / Swagger UI**
> - Swagger UI configurado en `/swagger-ui/index.html` con la dependencia `springdoc-openapi-starter-webmvc-ui:2.8.5`.

---

## Pruebas de Integración End-to-End (Validación JSON Estricta)

> [!IMPORTANT]
> **Validación de Casos Felices sin Instanciación de Clases de Proyecto (`new`)**
> - Creación de `OrchestrationIntegrationTest.java` sobre un contexto completo `@SpringBootTest(webEnvironment = RANDOM_PORT)` y servidores WireMock mockeando `numerator-api` y `json-server`.
> - **Regla de Oro**: Las solicitudes y respuestas HTTP se envían y validan exclusivamente mediante cadenas JSON puras (`String.class`) y `JsonPath`, garantizando el cumplimiento estricto de contratos `snake_case` y `camelCase` sin instanciar objetos Java del proyecto (`new CreateTransactionRequest(...)`).

---

## Estructura de Paquetes (`com.tiendanube.orchestration`)

```
src/main/java/com/tiendanube/orchestration/
├── config/
│   ├── RestClientConfig.java          # Configuración centralizada de RestClient con timeouts
│   └── OpenApiConfig.java             # Configuración de Swagger UI / OpenAPI 3.0
├── domain/
│   ├── Transaction.java               # Modelo de Transacción (datos y enmascaramiento)
│   ├── Receivable.java                # Modelo de Receivable (cálculo de comisiones y totales)
│   ├── PaymentMethod.java             # Enum rico con Lombok @Getter (Débito: 2% D+0, Crédito: 4% D+30)
│   └── CardDetails.java               # Value Object PCI-DSS (validación y enmascaramiento)
├── controller/
│   ├── request/
│   │   └── CreateTransactionRequest.java  # DTO Inmutable (Record) con Jakarta Validation
│   ├── response/
│   │   ├── TransactionResponse.java
│   │   ├── ReceivableResponse.java
│   │   └── OrchestrationResponse.java
│   ├── TransactionController.java     # Controller HTTP @RestController anotado con OpenAPI
│   └── GlobalExceptionHandler.java    # Handler centralizado de errores RFC 7807
├── client/
│   ├── NumeratorClient.java           # Cliente HTTP para test-and-set con reintentos y jitter
│   └── PersistenceClient.java         # Cliente HTTP para json-server (POST y DELETE de compensación)
├── filter/
│   └── TracingFilter.java             # Extracción e inyección de MDC (X-Request-Id / X-Trace-Id)
└── service/
    └── OrchestrationService.java      # Coordinador de orquestación de negocio
```
