# WebSocket Notifications - Guia Completo

## Problema Resolvido

✅ Antes: Notificações com polling (cron tabs) a cada X segundos = ineficiente e lento
✅ Depois: Notificações **push em tempo real** via WebSocket = instantâneo e eficiente

## Arquitetura

```
Utilizador                WebSocket (STOMP)              Backend
┌─────────┐              ┌──────────────┐              ┌────────────┐
│ Cliente │◄─────────────┤ Notificação  │◄─────────────┤ Veículo    │
│ Vue 3   │              │ em Tempo     │              │ chega a    │
└─────────┘              │ Real         │              │ Paragem    │
                         └──────────────┘              └────────────┘
                              ▲
                              │
                         Envia notificação
                    quando servidor detecta
                          evento
```

## Componentes Criados

### 1. Backend

#### `WebSocketConfig.java` (Config)
- Ativa o protocolo WebSocket com STOMP
- Configura endpoints: `/ws` para conexão
- Suporta fallback com SockJS para navegadores antigos

```properties
# URL de conexão
ws://localhost:8080/ws
```

#### `NotificationWebSocketService.java` (Serviço)
- Gerencia o envio de notificações via WebSocket
- Métodos principais:
  - `notifyUser(userId, notification)` - envia notificação a um utilizador
  - `broadcastToAll(topic, message)` - envia a todos

#### `NotificationWebSocketController.java` (Controller)
- Endpoints STOMP:
  - `/app/subscribe` - subscrever a notificações
  - `/app/ping` - verificar conexão

#### `VehicleService.java` (Atualizado)
- Agora chama `notificationWebSocketService.notifyUser()` quando veículo chega
- **Nenhuma necessidade de polling ou cron tabs!**

### 2. Frontend

#### `useWebSocketNotifications.ts` (Composable)
- Hook Vue 3 para conectar e receber notificações
- Gerencia a conexão STOMP automaticamente
- Estados: `notifications`, `isConnected`, `error`

```typescript
const { notifications, isConnected, error } = useWebSocketNotifications();
```

#### `NotificationCenter.vue` (Componente)
- Mostra notificações como toasts no canto superior direito
- Ícones diferentes por tipo de veículo (🚌 bus, 🚇 metro, 🚆 comboio)
- Auto-remove após 5 segundos (customizável)
- Animações suaves

## Fluxo de Funcionamento

```
1. Frontend conecta ao WebSocket
   └─ Cliente abre conexão em ws://localhost:8080/ws
   └─ STOMP negocia protocolo
   └─ Cliente subscreve a /user/queue/notifications

2. Backend: Veículo chega a paragem
   └─ VehicleService.arrive() chamado
   └─ Encontra utilizadores interessados
   └─ Cria notificação
   └─ Persiste em BD (histórico)
   └─ Chama notificationWebSocketService.notifyUser()

3. Servidor envia notificação
   └─ WebSocket envia para /user/{userId}/queue/notifications
   └─ Cliente recebe instantaneamente

4. Frontend exibe notificação
   └─ Componente NotificationCenter.vue mostra toast
   └─ Auto-remove após 5 segundos
   └─ Utilizador pode clicar para fechar antes
```

## Como Usar

### Backend - Dependências

Adicionar ao `pom.xml`:

```xml
<!-- WebSocket / STOMP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Opcional: para melhor suporte a STOMP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-messaging</artifactId>
</dependency>
```

### Frontend - Dependências

```bash
# npm
npm install sockjs-client stompjs

# pnpm
pnpm add sockjs-client stompjs

# yarn
yarn add sockjs-client stompjs
```

### Frontend - Integrar no App

**Opção 1: Componente Global (Recomendado)**

Em `App.vue`:

```vue
<template>
  <NotificationCenter />
  <RouterView />
</template>

<script setup>
import NotificationCenter from '@/components/NotificationCenter.vue';
</script>
```

**Opção 2: Usar Composable em Qualquer Componente**

```vue
<script setup>
import { useWebSocketNotifications } from '@/composables/useWebSocketNotifications';

const { notifications, isConnected } = useWebSocketNotifications();
</script>

<template>
  <div v-if="isConnected">
    <div v-for="n in notifications" :key="n.id">
      {{ n.message }}
    </div>
  </div>
</template>
```

## Configuração (application.properties)

```properties
# WebSocket
server.port=8080
spring.websocket.message-broker.enabled=true

# CORS (permitir conexões de outros domínios - apenas em dev)
server.servlet.context-path=/
```

## Testes

### 1. Verificar Conexão WebSocket

```bash
# Terminal 1: Backend
cd backend
mvn spring-boot:run

# Terminal 2: Frontend
cd frontend/catchit-mobile
npm install
npm run dev

# Terminal 3: Abrir browser
# Ir a http://localhost:5173
# Abrir DevTools > Console
# Deve ver: "WebSocket conectado"
```

### 2. Simular Notificação (curl)

```bash
# Obter token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123"}' | jq -r '.token')

# Listar veículos e paragens
curl http://localhost:8080/api/vehicles | jq '.[0].id'
curl http://localhost:8080/api/stops | jq '.[0].id'

# Simular chegada de veículo
curl -X POST http://localhost:8080/api/vehicles/{vehicleId}/arrive/{stopId} \
  -H "Authorization: Bearer $TOKEN"

# Deve ver notificação no frontend instantaneamente!
```

### 3. Teste de Carga

```bash
# Enviar múltiplas notificações
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/vehicles/{vehicleId}/arrive/{stopId} \
    -H "Authorization: Bearer $TOKEN" &
done
```

## Vantagens

| Aspecto | Polling (Antes) | WebSocket (Depois) |
|--------|-----------------|-------------------|
| **Latência** | 5-30 segundos | < 100ms |
| **Carga Servidor** | Alta (muitas requisições) | Baixa (apenas push) |
| **Banda** | Consumo constante | Apenas quando há eventos |
| **Experiência UX** | Atrasada | Instantânea |
| **Escalabilidade** | Limitada | Excelente |

## Troubleshooting

### Notificação não recebida?

1. **Verificar conexão WebSocket**
   ```javascript
   // No console do browser
   console.log(isConnected.value) // deve ser true
   ```

2. **Verificar logs do backend**
   ```
   Procurar por "WebSocket conectado" ou erros de STOMP
   ```

3. **CORS bloqueando?**
   - Verificar se `setAllowedOrigins("*")` está em WebSocketConfig
   - Em produção, usar domínios específicos

4. **Firewall bloqueando WebSocket?**
   - WebSocket usa porta 8080 por padrão
   - Pode ser bloqueado por proxies corporativos
   - Fallback para SockJS ativa automaticamente

### Performance Issues?

- Aumentar heartbeat em WebSocketConfig se houver timeouts
- Adicionar `nginx` reverse proxy para multiplexar WebSockets
- Implementar Redis pub/sub para scaling (ver seção avançada)

## Próximos Passos (Avançado)

### 1. Redis Pub/Sub para Múltiplos Servidores

```java
// Se tiver múltiplas instâncias, usar Redis:
config.enableStompBrokerRelay("/topic", "/queue")
      .setRelayHost("localhost")
      .setRelayPort(61613)
      .setSystemLogin("guest")
      .setSystemPasscode("guest");
```

### 2. Notificações Persistentes no Browser

```typescript
// Usar Service Worker para notificações mesmo com aba fechada
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/service-worker.js');
}
```

### 3. Múltiplos Tipos de Notificação

```typescript
// Subscrever a diferentes tópicos
stompClient.subscribe('/topic/alerts', handleAlert);
stompClient.subscribe('/user/queue/personal', handlePersonal);
stompClient.subscribe('/topic/broadcast', handleBroadcast);
```

## Referências

- [Spring WebSocket Docs](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [STOMP Protocol](https://stomp.github.io/)
- [Vue 3 Composables](https://vuejs.org/guide/extras/composition-api-faq.html#composables)

---

✅ **Status**: Implementação completa de notificações WebSocket push (sem polling)
