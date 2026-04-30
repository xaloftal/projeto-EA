# Notification Module

Este diretório documenta o sistema de notificações do backend baseado no padrão Observer.

## Objetivo

Notificar um utilizador quando um veículo chega a uma paragem que ele marcou como ponto de interesse (POI).

## Papéis principais

- `Stop` atua como `Subject`.
- `User` atua como `Observer`.
- `UserNotification` representa a notificação persistida no backend.

## Fluxo

1. O utilizador associa-se a uma paragem.
2. A paragem regista o utilizador como observer.
3. Quando o veículo chega à paragem, o backend dispara o evento.
4. O utilizador recebe uma notificação associada à sua conta.

## Endpoints ligados a este módulo

- `POST /api/stops/{stopId}/observers/{userId}`
- `DELETE /api/stops/{stopId}/observers/{userId}`
- `GET /api/stops/{stopId}/observers`
- `POST /api/stops/{stopId}/notify`
- `POST /api/vehicles/{vehicleId}/arrive/{stopId}`

## Implementação atual

- A ligação entre utilizador e paragem é feita através de POIs (`users_poi`).
- A notificação é criada em `User.notifyUser(...)` e persistida em `UserNotification`.
- A leitura das notificações usa BD como source of truth e Redis como cache de leitura.
- A cache é invalidada quando uma notificação é criada, eliminada ou quando um veículo chega a uma paragem relevante.
- Existe um processo agendado para retenção automática de notificações antigas na BD.

## Arquitetura híbrida

O módulo segue este modelo:

1. **BD como source of truth**
  - Todas as notificações vivem em `user_notifications`.
  - O histórico consultado pelo utilizador vem sempre da BD quando a cache não está disponível.

2. **Cache Redis para leitura rápida**
  - A lista de notificações por utilizador é guardada em Redis.
  - A chave segue o padrão `user:notifications:{userId}`.
  - A cache reduz leituras repetidas à BD para a caixa de notificações.

3. **Invalidação ao escrever**
  - Quando uma notificação é criada, a cache desse utilizador é limpa.
  - Quando uma notificação é apagada, a cache desse utilizador é limpa.
  - Quando um veículo chega a uma paragem, a cache dos utilizadores observadores é invalidada.

4. **Retenção automática na BD**
  - Um job agendado remove notificações com mais de X dias.
  - Por defeito, a retenção é de 60 dias.
  - Quando há eliminação em massa por retenção, toda a cache de notificações é limpa.

## Autenticação

Todos os endpoints requerem um JWT token válido no header `Authorization: Bearer {token}`.

Para obter um token:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "seu@email.com", "password": "senha"}'
```

Resposta:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

## Estrutura de dados

### Tabelas principais

- `user_notifications`: Registo de notificações por utilizador.
  - `id` (UUID): Identificador único.
  - `user_id` (UUID): Referência ao utilizador.
  - `stop_id` (UUID): ID da paragem onde o veículo chegou.
  - `stop_name` (VARCHAR): Nome da paragem (desnormalizado).
  - `message` (VARCHAR): Mensagem da notificação (ex: "O autocarro chegou à paragem ...").
  - `created_at` (TIMESTAMP): Data/hora de criação.

- `users_poi`: Tabela de associação ManyToMany entre utilizadores e paragens de interesse.
  - `user_id` (UUID): Referência ao utilizador.
  - `poi_id` (UUID): Referência à paragem.

## Cache e retenção

### Cache de notificações

- Serviço: `NotificationCacheService`
- Backend: Redis via `StringRedisTemplate`
- TTL por defeito: 5 minutos
- Configuração: `notifications.cache.ttl-minutes`

Comportamento:
- `findNotifications(userId)` tenta ler primeiro da cache.
- Se falhar, lê da BD, serializa e grava em Redis.
- `deleteNotification(...)` faz `evict(userId)` após remover da BD.
- `VehicleService.arrive(...)` invalida a cache dos utilizadores observadores afetados.

### Retenção automática

- Serviço: `NotificationRetentionService`
- Agendamento: todos os dias às 03:00 por defeito
- Configuração cron: `notifications.retention.cron`
- Dias de retenção por defeito: 60
- Configuração: `notifications.retention.days`

Comportamento:
- O job apaga notificações antigas da BD com base em `createdAt`.
- Se houver eliminações, a cache inteira das notificações é limpa para evitar leituras stale.

## Teste ponta-a-ponta

### 1. Obter token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "seu@email.com", "password": "senha"}' | jq -r '.token'
```

Guarda o token numa variável:
```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

### 2. Listar paragens disponíveis

```bash
curl http://localhost:8080/api/stops | jq -r '.[] | "\(.id) - \(.name)"' | head -5
```

Escolhe um `stopId`.

### 3. Subscrever a uma paragem

```bash
curl -i -X POST http://localhost:8080/api/stops/{stopId}/observers/{userId} \
  -H "Authorization: Bearer $TOKEN"
```

Esperado: `HTTP/1.1 200`

### 4. Confirmar subscrição na BD

```bash
docker exec -it catchit-db psql -U postgres -d catchitdb -c \
  "SELECT * FROM users_poi WHERE poi_id = '{stopId}';"
```

Deves ver um registo com o `user_id`.

### 5. Listar veículos

```bash
curl http://localhost:8080/api/vehicles | jq -r '.[] | "\(.id) - \(.type)"' | head -5
```

Escolhe um `vehicleId`.

### 6. Simular chegada do veículo

```bash
curl -i -X POST http://localhost:8080/api/vehicles/{vehicleId}/arrive/{stopId} \
  -H "Authorization: Bearer $TOKEN"
```

Esperado: `HTTP/1.1 200` com resposta JSON do veículo atualizado.

### 7. Confirmar notificação na BD

```bash
docker exec -it catchit-db psql -U postgres -d catchitdb -c \
  "SELECT id, user_id, stop_name, message, created_at FROM user_notifications WHERE user_id = '{userId}' ORDER BY created_at DESC LIMIT 5;"
```

Deves ver um registo com `stop_name` e uma mensagem tipo "O autocarro chegou à paragem ...".

## Detalhes de implementação

### Classes principais

- **Stop.java** (`Subject`)
  - Guarda lista transitória de `observers`.
  - `notifyObservers()`: Itera sobre todos os observers e chama `notifyUser()`.
  - `addObserver()` / `removeObserver()`: Gerencia a lista de observers.

- **User.java** (`Observer`)
  - Implementa `notifyUser(Subject stop)`.
  - Guarda lista persistida de `poi` (paragens de interesse).
  - Guarda lista de `notifications`.
  - Métodos utilitários: `addPOI()`, `removePOI()`, `hasPOI()`.

- **NotificationCacheService.java**
  - Faz cache por utilizador com TTL em Redis.
  - Serializa e desserializa listas de `UserNotification`.
  - Permite `get`, `put`, `evict` e `evictAll`.

- **NotificationRetentionService.java**
  - Executa limpeza periódica de notificações antigas.
  - Usa `@Scheduled` e uma data limite calculada a partir de `notifications.retention.days`.

- **UserNotification.java** (Entidade)
  - Representa uma notificação enviada a um utilizador.
  - Associada a `User` via ManyToOne.
  - Contém mensagem, paragem (stop_id, stop_name) e timestamp.

- **StopService.java**
  - `addObserver()`: Subscreve um utilizador a uma paragem.
  - `removeObserver()`: Desinscreve.
  - `getObservers()`: Lista utilizadores interessados.
  - `notifyObservers()`: Dispara manualmente.

- **VehicleService.java**
  - `arrive()`: Simula chegada do veículo a uma paragem.
    - Carrega observers da paragem.
    - Regista-os como observers do Stop.
    - Chama `vehicle.arrived()`.
    - Persiste mudanças.
    - Invalida a cache das notificações dos utilizadores afetados.

- **UserService.java**
  - `findNotifications()`: lê da cache ou da BD.
  - `deleteNotification()`: remove uma notificação e invalida a cache do utilizador.

## Status de compilação

✅ Backend compila sem erros.
✅ Endpoints HTTP testados com sucesso.
✅ ManyToMany em `users_poi` funcional.
✅ Notificações persistidas em `user_notifications`.
✅ Cache Redis de notificações funcional.
✅ Retenção automática de notificações antigas ativa.

## Próximos passos possíveis

- Adicionar endpoint `GET /api/users/{userId}/notifications` para listar notificações via HTTP.
- Expor notificações em tempo real via WebSocket.
- Adicionar preferência de canais de notificação por utilizador.
- Implementar suporte para email/SMS/push notifications.
