# Setup WebSocket Notifications - Passo a Passo

## 1. Adicionar Dependências ao pom.xml

Abrir `/pom.xml` e adicionar estas dependências na secção `<dependencies>`:

```xml
<!-- WebSocket e Messaging -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-messaging</artifactId>
</dependency>
```

**Localização no pom.xml**: Junto com as outras dependências (ex: spring-boot-starter-web, spring-boot-starter-data-jpa)

Exemplo:
```xml
<dependencies>
    <!-- Spring Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- WebSocket e Messaging (NOVO) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-messaging</artifactId>
    </dependency>

    <!-- Outras dependências... -->
</dependencies>
```

## 2. Recarregar Projeto Maven

**IntelliJ IDEA / VS Code:**
1. Abrir o terminal ou usar Maven view
2. Executar: `mvn clean install`
3. Ou: Clicar botão direito no pom.xml > Maven > Reload Projects

**VS Code com Extension Maven:**
1. Clicar em Explorer
2. Procurar por Maven
3. Clicar em "Reload Projects"

## 3. Verificar Imports

Após adicionar as dependências:
1. Os erros de import desaparecerão automaticamente
2. Os ficheiros compilarão sem erros
3. Fazer Full Build (Ctrl+Shift+F9 ou Maven clean + build)

## 4. Configurar application.properties (Opcional)

Adicionar ao `backend/src/main/resources/application.properties`:

```properties
# WebSocket Configuration
server.port=8080

# STOMP Message Broker
spring.websocket.message-broker.simple-enabled=true

# Para produção, adicionar:
# spring.websocket.cors-allowed-origins=https://seu-dominio.com
```

## 5. Validar Setup

### Opção A: Build Maven
```bash
cd backend
mvn clean install
```

Esperado: `BUILD SUCCESS`

### Opção B: Executar Backend
```bash
cd backend
mvn spring-boot:run
```

Esperado: Nenhum erro, servidor inicia em `http://localhost:8080`

### Opção C: Compilação em IDE
- IntelliJ: Build > Build Project
- VS Code: Pressionar Ctrl+Shift+B

## 6. Frontend - Instalar Dependências

```bash
cd frontend/catchit-mobile

# npm
npm install sockjs-client stompjs

# ou pnpm
pnpm add sockjs-client stompjs

# ou yarn
yarn add sockjs-client stompjs
```

## 7. Integrar Componente Frontend

Em `frontend/catchit-mobile/src/App.vue`:

```vue
<template>
  <!-- NOVO: Adicionar linha abaixo (antes do router-view) -->
  <NotificationCenter />
  <RouterView />
</template>

<script setup>
import NotificationCenter from '@/components/NotificationCenter.vue';
</script>
```

## 8. Testar

**Terminal 1: Backend**
```bash
cd backend
mvn spring-boot:run
```

**Terminal 2: Frontend**
```bash
cd frontend/catchit-mobile
npm run dev
```

**Browser:** Abrir `http://localhost:5173`

**DevTools Console:** Procurar por:
```
WebSocket conectado
```

Se ver esta mensagem, tudo está funcionando! ✅

## 9. Testar Notificações

```bash
# Terminal 3: Simular notificação

# Obter token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123"}' | jq -r '.token')

# Listar IDs
curl http://localhost:8080/api/vehicles | jq '.[0].id' # copiar vehicleId
curl http://localhost:8080/api/stops | jq '.[0].id'    # copiar stopId

# Simular chegada
curl -X POST http://localhost:8080/api/vehicles/{vehicleId}/arrive/{stopId} \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** Notificação aparece instantaneamente no canto superior direito do browser! 🎉

## Troubleshooting

### Erro: "package org.springframework.messaging cannot be found"
- ✅ **Solução**: Adicionar dependências ao pom.xml (passo 1)
- Execute `mvn clean install`

### Erro: "SimpMessagingTemplate cannot be resolved"
- ✅ **Solução**: Mesmo que acima - dependência não foi instalada
- Limpar cache: Delete `.m2` ou use `mvn clean`

### Frontend: "Cannot find module 'sockjs-client'"
- ✅ **Solução**: `npm install sockjs-client stompjs`

### WebSocket não conecta
- ✅ Verificar se backend está em `http://localhost:8080`
- ✅ Verificar CORS: `setAllowedOrigins("*")` em WebSocketConfig
- ✅ Verificar firewall/proxy bloqueando WebSocket

### Notificação não aparece
- ✅ Verificar console do browser (DevTools)
- ✅ Verificar se utilizador está subscrito a uma paragem (POI)
- ✅ Verificar logs do backend

## Ficheiros Criados/Modificados

### Backend
- ✅ `WebSocketConfig.java` - Configuração
- ✅ `NotificationWebSocketService.java` - Lógica de notificações
- ✅ `NotificationWebSocketController.java` - Endpoints STOMP
- ✅ `VehicleService.java` - Atualizado para enviar via WebSocket

### Frontend
- ✅ `useWebSocketNotifications.ts` - Composable Vue 3
- ✅ `NotificationCenter.vue` - Componente de UI

### Documentação
- ✅ `WEBSOCKET_NOTIFICATIONS.md` - Guia completo

## Próximas Otimizações (Opcional)

1. **Redis Pub/Sub** - Para múltiplos servidores
2. **Service Worker** - Para notificações offline
3. **Preferências por Utilizador** - Som, visual, email
4. **Histórico de Notificações** - Página com todas as notificações recebidas

---

✅ **Setup Completo** - Notificações WebSocket funcional!
