import { ref, onMounted, onUnmounted } from 'vue';

/**
 * Composable Vue 3 para receber notificações em tempo real via WebSocket/STOMP.
 * 
 * Uso:
 * ```vue
 * <script setup>
 * import { useWebSocketNotifications } from '@/composables/useWebSocketNotifications';
 * const { notifications, isConnected, error } = useWebSocketNotifications();
 * </script>
 * 
 * <template>
 *   <div v-if="isConnected" class="connection-ok">Conectado</div>
 *   <div v-for="notif in notifications" :key="notif.id" class="notification">
 *     {{ notif.message }}
 *   </div>
 * </template>
 * ```
 */
export const useWebSocketNotifications = () => {
  const notifications = ref([]);
  const isConnected = ref(false);
  const error = ref(null);
  let stompClient = null;
  let subscription = null;

  const connect = () => {
    return new Promise((resolve, reject) => {
      // Importar SockJS e STOMP (adicionar ao package.json se necessário)
      import('sockjs-client').then((SockJS) => {
        import('stompjs').then((Stomp) => {
          const socket = new SockJS.default('http://localhost:8080/ws');
          stompClient = Stomp.Stomp.over(socket);

          stompClient.connect({}, () => {
            isConnected.value = true;
            error.value = null;
            console.log('WebSocket conectado');

            // Subscrever a fila pessoal de notificações
            subscription = stompClient.subscribe('/user/queue/notifications', (message) => {
              const notification = JSON.parse(message.body);
              notifications.value.push(notification);
              console.log('Notificação recebida:', notification);

              // Opcional: remover notificação após 5 segundos
              setTimeout(() => {
                notifications.value = notifications.value.filter(n => n.id !== notification.id);
              }, 5000);
            });

            resolve();
          }, (error) => {
            isConnected.value = false;
            error.value = error;
            console.error('Erro ao conectar WebSocket:', error);
            reject(error);
          });
        });
      });
    });
  };

  const disconnect = () => {
    if (subscription) {
      subscription.unsubscribe();
    }
    if (stompClient && stompClient.connected) {
      stompClient.disconnect(() => {
        isConnected.value = false;
        console.log('WebSocket desconectado');
      });
    }
  };

  const sendMessage = (destination, message) => {
    if (stompClient && stompClient.connected) {
      stompClient.send(destination, {}, JSON.stringify(message));
    }
  };

  const clearNotifications = () => {
    notifications.value = [];
  };

  onMounted(() => {
    connect().catch((e) => {
      console.error('Falha ao conectar ao WebSocket:', e);
    });
  });

  onUnmounted(() => {
    disconnect();
  });

  return {
    notifications,
    isConnected,
    error,
    connect,
    disconnect,
    sendMessage,
    clearNotifications,
  };
};
