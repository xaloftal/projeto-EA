# Payment Service - End-to-End

Este documento resume o fluxo de pagamento completo implementado no projeto, desde o checkout no backend ate a persistencia de transacoes e eventos no payment-service.

## Objetivo

O payment-service foi isolado para melhorar escalabilidade e resiliencia.

Responsabilidades principais:
- Autorizar pagamentos.
- Capturar pagamentos autorizados.
- Processar reembolsos.
- Persistir transacoes e historico de eventos.
- Aplicar regras de maquina de estados para manter consistencia.

## Arquitetura

Componentes envolvidos:
- backend: cria sessao de checkout, calcula total e chama o payment-service.
- payment-service: decide autorizacao via Strategy, aplica transicoes de estado e persiste dados.
- PostgreSQL: armazena transacoes e eventos.
- Redis: continua no backend para sessao/carrinho (nao para transacoes de pagamento).

Containers no Compose:
- db
- redis
- backend
- payment-service
- frontend

## Fluxo Completo (Inicio ao Fim)

1. Cliente confirma checkout no backend (`/api/checkout/confirm`).
2. Backend valida sessao de checkout no Redis.
3. Backend gera `orderId` e monta payload de pagamento com:
- orderId
- userId
- sessionId
- paymentMethodId
- amount
- currency
4. Backend chama `POST /api/payments/authorize` no payment-service.
5. Payment-service cria transacao em `PENDING`.
6. Payment-service escolhe Strategy de pagamento por `paymentMethodId`.
7. Resultado da Strategy:
- aprovado: `PENDING -> AUTHORIZED`, cria evento `AUTHORIZED`.
- rejeitado: `PENDING -> DECLINED`, cria evento `FAILED` e guarda `failureReason`.
8. Backend recebe resposta:
- aprovado: debita saldo do utilizador, limpa carrinho/sessao e confirma pedido.
- rejeitado: devolve erro de pagamento.
9. Operacoes posteriores:
- captura: `POST /api/payments/{orderId}/capture` faz `AUTHORIZED -> CAPTURED` e evento `CAPTURED`.
- reembolso: `POST /api/payments/{orderId}/refund` em pagamento `CAPTURED`.
10. Reembolso:
- parcial: mantem `CAPTURED`, aumenta `refundedAmount`, evento `REFUNDED`.
- total: `CAPTURED -> REFUNDED`, evento `REFUNDED`.
11. Consulta de estado:
- `GET /api/payments/{orderId}` retorna estado atual e metadados da transacao.

## Validacao de Saldo (Backend Checkout)

Antes de chamar autorizacao no payment-service, o backend valida se o saldo do utilizador e suficiente para o total do checkout.

Regras:
- Se `saldo < total`, o checkout e bloqueado com `402 PAYMENT_REQUIRED`.
- Se o pagamento for aprovado, o backend debita o saldo e persiste o novo valor na entidade `User`.

Concorrencia:
- A leitura do utilizador para debito e feita com lock pessimista para reduzir risco de duas compras concorrentes consumirem o mesmo saldo.

## Maquina de Estados

Estados:
- PENDING
- AUTHORIZED
- CAPTURED
- DECLINED
- REFUNDED

Transicoes validas:
- `PENDING -> AUTHORIZED`
- `PENDING -> DECLINED`
- `AUTHORIZED -> CAPTURED`
- `CAPTURED -> REFUNDED`

Transicoes invalidas disparam erro de dominio e resposta HTTP de conflito quando aplicavel.

## Persistencia de Dados

### Entidade `PaymentTransaction`

Campos principais:
- orderId
- userId
- amount
- currency
- method
- status
- providerTransactionId
- failureReason
- refundedAmount
- createdAt
- updatedAt

Tabela: `payment_transactions`

### Entidade `PaymentTransactionEvent`

Campos principais:
- transaction_id
- eventType
- amount
- message
- createdAt

Tabela: `payment_transaction_events`

Tipos de evento:
- AUTHORIZED
- CAPTURED
- FAILED
- REFUNDED

## Endpoints

Base path: `/api/payments`

- `POST /authorize`
- `POST /{orderId}/capture`
- `POST /{orderId}/refund`
- `GET /{orderId}`

## Strategy de Pagamento

A escolha da Strategy e feita por suporte ao `paymentMethodId`.

Implementacoes atuais:
- `BalancePaymentStrategy`
- `FallbackDeclinePaymentStrategy`

## Resiliencia no Backend

O backend usa Circuit Breaker no cliente HTTP para o payment-service.

Comportamento esperado:
- falhas consecutivas abrem o circuito.
- chamadas passam a falhar rapidamente (fail-fast) enquanto circuito aberto.
- fallback converte falha em indisponibilidade de servico para o fluxo de checkout.

## Configuracao

`payment-service` usa variaveis de ambiente para base de dados:
- `PAYMENT_DB_URL`
- `PAYMENT_DB_USER`
- `PAYMENT_DB_PASSWORD`

No Docker Compose, por default o payment-service aponta para o mesmo PostgreSQL do projeto.

## Estado Atual de Implementacao

Ja implementado:
- servico isolado em container.
- autorizacao/captura/reembolso com persistencia.
- eventos de pagamento persistidos.
- maquina de estados com transicoes explicitas.
- integracao com checkout do backend.
- circuit breaker no backend para chamadas ao payment-service.
- validacao de saldo e debito no backend apos autorizacao aprovada.

Ainda recomendado para evolucao:
- idempotencia por chave para evitar dupla cobranca em retries.
- testes de integracao para transicoes de estado.
- reconciliacao com gateway externo real (Stripe/Adyen/etc).
- observabilidade com metricas e tracing.

## Fundamentacao das Decisoes

### Porque isolar o pagamento

- Escalabilidade independente: picos de checkout nao obrigam a escalar o backend inteiro.
- Isolamento de risco: falhas do pagamento ficam confinadas e podem ser tratadas por circuit breaker.
- Evolucao tecnica: permite trocar/adicionar metodos de pagamento sem acoplar a camada de dominio principal.
- Auditoria: historico de eventos de pagamento fica centralizado num bounded context proprio.

### Porque manter Strategy dentro do payment-service

- O backend nao precisa conhecer regras de autorizacao por metodo.
- A adicao de novos metodos fica local (Open/Closed Principle).
- A decisao final de aprovacao/rejeicao fica perto da persistencia da transacao.

### Porque manter validacao de saldo no backend

- O saldo pertence ao dominio de utilizador/conta no backend.
- Evita duplicacao de regras de negocio entre servicos.
- Permite devolver erro de negocio imediato antes de continuar o fluxo.

## Linha do Tempo da Implementacao

1. Isolamento inicial do `payment-service` em container e integracao no `docker-compose`.
2. Migracao da autorizacao para chamada HTTP backend -> payment-service.
3. Introducao do Strategy (`BalancePaymentStrategy` + fallback de rejeicao).
4. Persistencia com JPA (`PaymentTransaction` e `PaymentTransactionEvent`).
5. Maquina de estados para garantir transicoes validas.
6. Circuit breaker no backend para tolerancia a falhas do payment-service.
7. Endpoint de validacao por `orderId` no backend para confirmar status de pagamento.
8. Integracao no frontend para mostrar sucesso apenas quando `valid=true`.
9. Reforco de regra de negocio: metodo aceito apenas `balance-*`.
10. Exposicao correta de erros de saldo insuficiente no frontend.
11. Correcao de lock pessimista: lock dedicado ao checkout, sem bloquear login/perfil.

## Integracao Frontend e Regras de UX

- Checkout envia `paymentMethodId` selecionado para `/api/checkout/confirm`.
- Fluxo final aceite: apenas metodos com prefixo `balance-`.
- Pagina de sucesso nao confia so no `orderId` na URL; chama backend para validar estado do pagamento.
- A pagina de sucesso so e renderizada quando `valid=true`.
- Quando o saldo e insuficiente, o frontend mostra erro de negocio retornado pelo backend.

## Correcao de Problemas Reais (Licoes)

### 1. Compras aprovadas sem saldo suficiente

Problema:
- o fluxo de checkout autorizava sem comparar total com saldo do utilizador.

Correcao:
- validacao `saldo >= total` antes de autorizar.
- retorno `402 PAYMENT_REQUIRED` quando nao ha saldo.
- debito de saldo apenas apos autorizacao aprovada.

### 2. Sucesso de pagamento sem validacao real

Problema:
- a rota de sucesso podia ser aberta sem prova de pagamento validado.

Correcao:
- endpoint de validacao por pedido no backend.
- frontend passou a exigir `valid=true` para renderizar sucesso.

### 3. Lock pessimista a afetar login/perfil

Problema:
- lock em `findById` do repositorio de utilizador afetou leituras gerais e gerou erro `could not obtain pessimistic lock`.

Correcao:
- `findById` mantido para leitura normal.
- metodo dedicado com lock apenas para checkout e debito de saldo concorrente.

## Checklist de Validacao (Manual)

1. Subir stack:
- `docker compose up --build`
2. Criar sessao de checkout:
- `POST /api/checkout/session`
3. Confirmar checkout com saldo suficiente:
- `POST /api/checkout/confirm`
- esperar `approved=true` e `orderId` preenchido.
4. Validar pedido:
- `GET /api/checkout/orders/{orderId}/validation`
- esperar `valid=true` para pagamentos aprovados.
5. Confirmar persistencia:
- verificar `payment_transactions` e `payment_transaction_events`.
6. Testar saldo insuficiente:
- repetir checkout com saldo abaixo do total.
- esperar erro `402 PAYMENT_REQUIRED` e mensagem clara no frontend.
7. Testar login/perfil apos checkout concorrente:
- garantir ausencia do erro de lock pessimista nas leituras de utilizador.

## Limites Atuais Conhecidos

- Sem integracao com gateway real: autorizacao ainda e simulada por Strategy local.
- Sem idempotency key no endpoint de autorizacao (risco em retries agressivos).
- Sem outbox/event bus para propagacao assicrona de eventos de pagamento.
- Sem dashboard de observabilidade (metricas de latencia, taxa de declinio, retries).

