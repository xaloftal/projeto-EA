package PSM.UserManagement.notification;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import PSM.UserManagement.UserNotification;

/**
 * Serviço para enviar notificações em tempo real via WebSocket.
 * 
 * Propósito: Notificar utilizadores quando há eventos (ex: veículo chega a paragem)
 * sem necessidade de polling ou cron tabs.
 * 
 * Exemplo de uso:
 * notificationWebSocketService.notifyUser(userId, notification);
 */
@Service
public class NotificationWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public NotificationWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Envia uma notificação a um utilizador específico via WebSocket.
     * 
     * @param userId ID do utilizador a notificar
     * @param notification A notificação a enviar
     */
    public void notifyUser(UUID userId, UserNotification notification) {
        if (userId == null || notification == null) {
            return;
        }
        
        try {
            // Envia para a fila específica do utilizador: /user/{userId}/queue/notifications
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
        } catch (Exception e) {
            // Log de erro mas não falha a transação
            System.err.println("Erro ao enviar WebSocket notification para utilizador " + userId + ": " + e.getMessage());
        }
    }
    
    /**
     * Envia uma notificação de teste (para debugging).
     * 
     * @param userId ID do utilizador
     * @param message Mensagem de teste
     */
    public void sendTestNotification(UUID userId, String message) {
        if (userId == null || message == null) {
            return;
        }
        
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/test",
                    message
            );
        } catch (Exception e) {
            System.err.println("Erro ao enviar test notification: " + e.getMessage());
        }
    }
    
    /**
     * Broadcast a todos os utilizadores conectados (atualizações gerais).
     * 
     * @param topic Tópico (ex: "updates", "alerts")
     * @param message Mensagem a enviar
     */
    public void broadcastToAll(String topic, Object message) {
        if (topic == null || message == null) {
            return;
        }
        
        try {
            messagingTemplate.convertAndSend("/topic/" + topic, message);
        } catch (Exception e) {
            System.err.println("Erro ao fazer broadcast para topic " + topic + ": " + e.getMessage());
        }
    }
}
