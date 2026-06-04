<template>
  <!-- Notification Toast Container (canto superior direito) -->
  <div class="notification-container">
    <!-- Status da Conexão -->
    <div class="connection-status" :class="{ connected: isConnected, disconnected: !isConnected }">
      <span v-if="isConnected" class="status-badge">✓ Conectado</span>
      <span v-else class="status-badge">✗ Desconectado</span>
    </div>

    <!-- Notificações em Toast -->
    <TransitionGroup name="notification-toast" tag="div" class="toast-list">
      <div
        v-for="notif in notifications"
        :key="notif.id"
        class="notification-toast"
        :class="{ 'has-vehicle-type': notif.vehicleType }"
      >
        <!-- Ícone baseado no tipo de veículo -->
        <div class="notification-icon">
          <span v-if="notif.vehicleType === 'BUS'" class="vehicle-icon">🚌</span>
          <span v-else-if="notif.vehicleType === 'METRO'" class="vehicle-icon">🚇</span>
          <span v-else-if="notif.vehicleType === 'TRAIN'" class="vehicle-icon">🚆</span>
          <span v-else class="vehicle-icon">📍</span>
        </div>

        <!-- Conteúdo da Notificação -->
        <div class="notification-content">
          <p class="notification-message">{{ notif.message }}</p>
          <p v-if="notif.stopName" class="notification-detail">{{ notif.stopName }}</p>
          <p v-if="notif.routeName" class="notification-detail">Linha: {{ notif.routeName }}</p>
          <p class="notification-time">{{ formatTime(notif.createdAt) }}</p>
        </div>

        <!-- Botão Fechar -->
        <button class="close-btn" @click="removeNotification(notif.id)">×</button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { useWebSocketNotifications } from '@/composables/useWebSocketNotifications';

interface Notification {
  id: string;
  message: string;
  stopName?: string;
  stopId?: string;
  routeName?: string;
  vehicleType?: 'BUS' | 'METRO' | 'TRAIN';
  createdAt?: string;
}

const { notifications, isConnected } = useWebSocketNotifications();

const removeNotification = (notificationId: string) => {
  const index = notifications.value.findIndex((n: Notification) => n.id === notificationId);
  if (index > -1) {
    notifications.value.splice(index, 1);
  }
};

const formatTime = (dateString?: string): string => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' });
};
</script>

<style scoped>
.notification-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  max-width: 400px;
  font-family: system-ui, -apple-system, sans-serif;
}

.connection-status {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  animation: fadeIn 0.3s ease;
}

.connection-status.connected {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.connection-status.disconnected {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.toast-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-toast {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-left: 4px solid #007bff;
  animation: slideIn 0.3s ease;
  min-width: 300px;
}

.notification-toast.has-vehicle-type {
  border-left-color: #28a745;
}

.notification-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: #f0f0f0;
  border-radius: 50%;
  font-size: 20px;
}

.vehicle-icon {
  display: block;
}

.notification-content {
  flex-grow: 1;
  min-width: 0;
}

.notification-message {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  word-wrap: break-word;
}

.notification-detail {
  margin: 2px 0;
  font-size: 12px;
  color: #666;
}

.notification-time {
  margin: 4px 0 0 0;
  font-size: 11px;
  color: #999;
}

.close-btn {
  flex-shrink: 0;
  background: none;
  border: none;
  font-size: 24px;
  color: #ccc;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #999;
}

/* Animações */
@keyframes slideIn {
  from {
    transform: translateX(400px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.notification-toast-enter-active,
.notification-toast-leave-active {
  transition: all 0.3s ease;
}

.notification-toast-enter-from {
  transform: translateX(400px);
  opacity: 0;
}

.notification-toast-leave-to {
  transform: translateX(400px);
  opacity: 0;
}

/* Responsive */
@media (max-width: 600px) {
  .notification-container {
    top: 10px;
    right: 10px;
    left: 10px;
    max-width: 100%;
  }

  .notification-toast {
    min-width: 0;
  }
}
</style>
