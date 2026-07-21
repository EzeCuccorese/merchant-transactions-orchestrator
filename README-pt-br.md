# Orchestration API

## Opções de Idioma

Este documento também está disponível em:

- **English**: [README.md](README.md)
- **Español (Argentina)**: [README-es-ar.md](README-es-ar.md)

## Uso de Ferramentas de IA

O uso de ferramentas de IA (como GitHub Copilot, Cursor, Claude, ChatGPT ou qualquer outra) é totalmente válido e encorajado. Na Nuvemshop, a IA já faz parte do dia a dia dos nossos times de engenharia.
Na entrevista técnica, adoraremos conversar sobre como você abordou o desafio — incluindo como utilizou a IA ao longo do processo.

## Objetivo

O objetivo desta tarefa é construir uma API para orquestrar a criação de transações e recebíveis (como um serviço de pagamento).
É importante garantir a consistência dos dados.
Implemente a tarefa usando qualquer linguagem de programação ou framework de sua escolha (idealmente aquele com o qual você se sente mais confiante).
Sinta-se à vontade para usar as ferramentas e tecnologias com as quais você se sente mais confortável.

### Recursos Fornecidos

- Fornecemos uma API usando [json-server](https://github.com/typicode/json-server) que já inclui endpoints
  para gerenciar transações e recebíveis (não reimplemente-os). Você deve usar o json-server fornecido como banco de dados para fins de desenvolvimento.
  **Se você não estiver familiarizado com json-server, reserve um tempo para explorar suas funcionalidades antes de prosseguir com a tarefa**.
- Uma API para o Numerator Service para gerar IDs únicos. **NOTA: Esta é uma das partes mais relevantes do desafio.
  Você deve usar a API Numerator (não use nenhum outro serviço de geração de ID, como UUID). Existem diferentes maneiras de implementar isso - cada uma com suas próprias compensações -
  implemente a que você acha que funciona melhor para este cenário**

### Tarefa: Criar API de Orquestração

O objetivo desta tarefa é construir uma API que processa e armazena transações iniciadas por um merchant (comerciante), garantindo a dedução correta
de taxas e a criação dos recebíveis correspondentes.

Uma transação criada deve incluir:

- Um ID único criado usando a Numerator API - lembre-se de que esperamos uma escolha inteligente de uso aqui.
- O valor total da transação, formatado como uma string decimal.
- Uma descrição da transação, por exemplo, "Camiseta Preta M".
- Método de pagamento: **debit_card** ou **credit_card**.
- O número do cartão (apenas os últimos 4 dígitos devem ser armazenados e retornados, pois são informações sensíveis).
- O nome do portador do cartão.
- Data de expiração do cartão no formato MM/AA.
- CVV do cartão.

Ao criar uma transação, **um recebível do merchant também deve ser criado**, um recebível representa a porção
do valor da transação que vai para o merchant após deduzir a taxa aplicável.

#### Regras para Criar Recebíveis

| Tipo de Transação | Status do Recebível | Data de Pagamento                  | Taxa |
| ----------------- | ------------------- | ---------------------------------- | ---- |
| **Debit Card**    | `paid`              | Mesma data de criação (D + 0)      | 2%   |
| **Credit Card**   | `waiting_funds`     | Data de criação + 30 dias (D + 30) | 4%   |

**Exemplo**: Se um recebível for criado com um valor de 100,00 de uma transação com **credit_card**, o
merchant receberá 96,00 (a taxa é calculada com base no valor total da transação).

### Geração de ID Único com API Numerator

É essencial que **_transações e recebíveis_** tenham IDs únicos gerados. O **numerator service**
simula um sistema externo que ajuda você a implementar sua própria lógica de geração de ID.

**Nota**: A Numerator API retorna um número para os IDs gerados, mas o json-server espera strings para IDs, você precisará
converter o número para o formato string.

## Configuração

### Iniciar serviços fornecidos

```
docker compose up
```

Isso exporá:

1. Em http://0.0.0.0:8080/ a API para gerenciar transações e recebíveis
2. Em http://0.0.0.0:3000/ a API para geração de IDs.

## Visão Geral dos Serviços da API

### Transações

| Endpoint           | Método   | Descrição                                                           | Corpo da Requisição                                                                                                                                                                                     |
| ------------------ | -------- | ------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `transactions`     | `GET`    | Listar todas as transações.                                         | -                                                                                                                                                                                                       |
| `transactions/:id` | `GET`    | Obter detalhes de uma transação específica por ID.                  | -                                                                                                                                                                                                       |
| `transactions`     | `POST`   | Criar uma nova transação. Use numerator-api para gerar um ID único. | `{ "id": <string>, "value": "250.00", "description": "T-Shirt", "method": "credit_card", "cardNumber": "2222", "cardHolderName": "Simplenube Store", "cardExpirationDate": "04/28", "cardCvv": "222" }` |
| `transactions/:id` | `DELETE` | Excluir uma transação por ID.                                       | -                                                                                                                                                                                                       |

### Recebíveis

| Endpoint          | Método   | Descrição                                                          | Corpo da Requisição                                                                                                                                                              |
| ----------------- | -------- | ------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `receivables`     | `GET`    | Listar todos os recebíveis.                                        | -                                                                                                                                                                                |
| `receivables/:id` | `GET`    | Obter detalhes de um recebível específico por ID.                  | -                                                                                                                                                                                |
| `receivables`     | `POST`   | Criar um novo recebível. Use numerator-api para gerar um ID único. | `{ "id": <string>, "transaction_id": <string>, "status": "waiting_funds", "create_date": "2022-05-20T19:20:14.576-03:00", "subtotal": "250", "discount": "10", "total": "240" }` |
| `receivables/:id` | `DELETE` | Excluir um recebível por ID.                                       | -                                                                                                                                                                                |

### Numerator

Numerator é um serviço que fornece o ID atual e armazena o seguinte, que são necessários para criar transações e recebíveis.
Embora o serviço ofereça vários endpoints, você não é obrigado a usar todos eles. A implementação pode ser feita de várias maneiras dependendo da sua abordagem
para gerar IDs únicos.

| Endpoint                 | Método   | Descrição                                                                                                                                                                                                                                                                                                                                                                                                        | Corpo da Requisição                              |
| ------------------------ | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| `numerator`              | `GET`    | Recupera o valor atual do numerator, independentemente do status do bloqueio. Sempre retorna o valor atual, mesmo que o repositório esteja bloqueado.                                                                                                                                                                                                                                                            | -                                                |
| `numerator`              | `PUT`    | Define o valor do numerator para o `value` especificado imediatamente, sem verificar se o repositório está bloqueado.                                                                                                                                                                                                                                                                                            | `{ "value": <number> }`                          |
| `numerator/test-and-set` | `PUT`    | Define atomicamente o numerator para `newValue` se o valor atual corresponder a `oldValue`. Retorna `newValue` em caso de sucesso, ou erro HTTP 400 com um corpo `{ "error": "Numerator does not match the expected old value.", "currentNumerator": <number> }` se falhar. Esta operação é atômica, garantindo que a comparação e definição do novo valor não possam ser interrompidas por outras operações.    | `{ "oldValue": <number>, "newValue": <number> }` |
| `numerator/lock`         | `POST`   | Define a flag de bloqueio (`lock = true`) no repositório do Numerator. Há um parâmetro de timeout que é a quantidade de tempo que o sistema continuará tentando adquirir o bloqueio (por padrão é 10.000 milissegundos ou 10 segundos). Retorna 400 se não conseguir obter o lock antes de atingir o timeout. Apenas uma requisição pode manter o bloqueio por vez, e o bloqueio NÃO é liberado automaticamente. | `{ "timeout": <number, in milliseconds> }`       |
| `numerator/lock`         | `DELETE` | Libera o bloqueio definindo a flag de bloqueio como `false`. Se já estiver `false`, permanece inalterado.                                                                                                                                                                                                                                                                                                        | -                                                |
