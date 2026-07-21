# Orchestration API

## Opciones de Idioma

Este documento también está disponible en:

- **English**: [README.md](README.md)
- **Português (Brasil)**: [README-pt-br.md](README-pt-br.md)

## Uso de Herramientas de IA

El uso de herramientas de IA (como GitHub Copilot, Cursor, Claude, ChatGPT u otras) está completamente permitido y es bienvenido. En Tiendanube, la IA ya es parte del día a día de nuestros equipos de ingeniería.
En la entrevista técnica, nos interesará conversar sobre cómo abordaste el desafío, incluyendo cómo usaste la IA a lo largo del proceso.

## Objetivo

El objetivo de esta tarea es construir una API para orquestrar la creación de transacciones y cuentas por cobrar (como un servicio de pago).
Es importante asegurar la consistencia de los datos.
Implementa la tarea usando cualquier lenguaje de programación o framework de tu elección (idealmente el que te sientas más confiado).
Siéntete libre de usar las herramientas y tecnologías con las que te sientas más cómodo.

### Recursos Proporcionados

- Hemos proporcionado una API usando [json-server](https://github.com/typicode/json-server) que ya incluye endpoints
  para gestionar transacciones y cuentas por cobrar (no los reimplementes). Debes usar el json-server proporcionado como base de datos para propósitos de desarrollo.
  **Si no estás familiarizado con json-server, por favor toma un tiempo para explorar sus funcionalidades antes de proceder con la tarea**.
- Una API para el Numerator Service para generar IDs únicos. **NOTA: Esta es una de las partes más relevantes del desafío.
  Debes usar la Numerator API (no uses ningún otro servicio de generación de ID, como UUID). Hay diferentes maneras de implementar esto - cada una con sus propias compensaciones -
  implementa la que creas que funciona mejor para este escenario**

### Tarea: Crear API de Orquestración

El objetivo de esta tarea es construir una API que procese y almacene transacciones iniciadas por un merchant (comerciante), asegurando la deducción correcta
de comisiones y la creación de las cuentas por cobrar (receivables) correspondientes.

Una transacción creada debe incluir:

- Un ID único creado usando la Numerator API - recuerda que esperamos una elección inteligente de uso aquí.
- El monto total de la transacción, formateado como una cadena decimal.
- Una descripción de la transacción, por ejemplo, "Remera Negra M".
- Método de pago: **debit_card** o **credit_card**.
- El número de tarjeta (solo los últimos 4 dígitos deben ser almacenados y devueltos, ya que es información sensible).
- El nombre del titular de la tarjeta.
- Fecha de vencimiento de la tarjeta en formato MM/AA.
- CVV de la tarjeta.

Al crear una transacción, **una cuenta por cobrar del merchant también debe ser creada**, una cuenta por cobrar representa la porción
del monto de la transacción que va al merchant después de deducir la comisión aplicable.

#### Reglas para Crear Cuentas por Cobrar

| Tipo de Transacción | Estado de la Cuenta por Cobrar | Fecha de Pago                        | Comisión |
| ------------------- | ------------------------------ | ------------------------------------ | -------- |
| **Debit Card**      | `paid`                         | Misma fecha de creación (D + 0)      | 2%       |
| **Credit Card**     | `waiting_funds`                | Fecha de creación + 30 días (D + 30) | 4%       |

**Ejemplo**: Si una cuenta por cobrar es creada con un valor de ARS 100,00 de una transacción con **credit_card**, el
merchant recibirá ARS 96,00 (la comisión se calcula basada en el monto total de la transacción).

### Generación de ID Único con Numerator API

Es esencial que **_transacciones y cuentas por cobrar_** tengan IDs únicos generados. El **Numerator Service**
simula un sistema externo que te ayuda a implementar tu propia lógica de generación de ID.

**Nota**: La Numerator API retorna un número para los IDs generados pero json-server espera strings para IDs, necesitarás
convertir el número al formato string.

## Configuración

### Iniciar servicios proporcionados

```
docker compose up
```

Esto expondrá:

1. En http://0.0.0.0:8080/ la API para gestionar transacciones y cuentas por cobrar
2. En http://0.0.0.0:3000/ la API para generación de IDs.

## Resumen de Servicios de la API

### Transacciones

| Endpoint           | Método   | Descripción                                                              | Cuerpo de la Solicitud                                                                                                                                                                                  |
| ------------------ | -------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `transactions`     | `GET`    | Listar todas las transacciones.                                          | -                                                                                                                                                                                                       |
| `transactions/:id` | `GET`    | Obtener detalles de una transacción específica por ID.                   | -                                                                                                                                                                                                       |
| `transactions`     | `POST`   | Crear una nueva transacción. Usa Numerator API para generar un ID único. | `{ "id": <string>, "value": "250.00", "description": "T-Shirt", "method": "credit_card", "cardNumber": "2222", "cardHolderName": "Simplenube Store", "cardExpirationDate": "04/28", "cardCvv": "222" }` |
| `transactions/:id` | `DELETE` | Eliminar una transacción por ID.                                         | -                                                                                                                                                                                                       |

### Cuentas por Cobrar

| Endpoint          | Método   | Descripción                                                                    | Cuerpo de la Solicitud                                                                                                                                                           |
| ----------------- | -------- | ------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `receivables`     | `GET`    | Listar todas las cuentas por cobrar.                                           | -                                                                                                                                                                                |
| `receivables/:id` | `GET`    | Obtener detalles de una cuenta por cobrar específica por ID.                   | -                                                                                                                                                                                |
| `receivables`     | `POST`   | Crear una nueva cuenta por cobrar. Usa Numerator API para generar un ID único. | `{ "id": <string>, "transaction_id": <string>, "status": "waiting_funds", "create_date": "2022-05-20T19:20:14.576-03:00", "subtotal": "250", "discount": "10", "total": "240" }` |
| `receivables/:id` | `DELETE` | Eliminar una cuenta por cobrar por ID.                                         | -                                                                                                                                                                                |

### Numerator

Numerator Service es un servicio que proporciona el ID actual y almacena el siguiente, que son requeridos para crear transacciones y cuentas por cobrar.
Aunque el servicio ofrece varios endpoints, no estás obligado a usar todos ellos. La implementación puede hacerse de varias maneras dependiendo de tu enfoque
para generar IDs únicos.

| Endpoint                 | Método   | Descripción                                                                                                                                                                                                                                                                                                                                                                                                              | Cuerpo de la Solicitud                           |
| ------------------------ | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------ |
| `numerator`              | `GET`    | Recupera el valor actual del numerator, independientemente del estado del bloqueo. Siempre retorna el valor actual, incluso si el repositorio está bloqueado.                                                                                                                                                                                                                                                            | -                                                |
| `numerator`              | `PUT`    | Establece el valor del numerator al `value` especificado inmediatamente, sin verificar si el repositorio está bloqueado.                                                                                                                                                                                                                                                                                                 | `{ "value": <number> }`                          |
| `numerator/test-and-set` | `PUT`    | Establece atómicamente el numerator a `newValue` si el valor actual coincide con `oldValue`. Retorna `newValue` en caso de éxito, o error HTTP 400 con un cuerpo `{ "error": "Numerator does not match the expected old value.", "currentNumerator": <number> }` si falla. Esta operación es atómica, asegurando que la comparación y establecimiento del nuevo valor no puedan ser interrumpidas por otras operaciones. | `{ "oldValue": <number>, "newValue": <number> }` |
| `numerator/lock`         | `POST`   | Establece la bandera de bloqueo (`lock = true`) en el repositorio numerator. Hay un parámetro de timeout que es la cantidad de tiempo que el sistema seguirá intentando adquirir el bloqueo (por defecto es 10.000 milisegundos o 10 segundos). Retorna 400 si no se obtiene el lock antes de alcanzar el timeout. Solo una solicitud puede mantener el bloqueo a la vez, y el bloqueo NO se libera automáticamente.     | `{ "timeout": <number, in milliseconds> }`       |
| `numerator/lock`         | `DELETE` | Libera el bloqueo estableciendo la bandera de bloqueo a `false`. Si ya está `false`, permanece sin cambios.                                                                                                                                                                                                                                                                                                              | -                                                |
