package PSM.UserManagement.notification;

import java.util.UUID;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/**
 * Controller WebSocket/STOMP para gerenciar conexões e mensagens de notificação.
 * 
 * Endpoints STOMP:
 * - /app/subscribe -> /topic/updates (subscribe a atualizações)
 * - /app/ping -> responde pong
 * 
 * Fluxo:
 * 1. Cliente conecta a ws://localhost:8080/ws
 * 2. Cliente subscreve a /user/queue/notifications (pessoal)
 * 3. Servidor envia notificações via NotificationWebSocketService
 * 4. Cliente recebe notificações em tempo real
 */
@Controller
public class NotificationWebSocketController {
    
    /**
     * Endpoint para ping/pong (verificar se conexão está viva).
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String ping() {
        return "pong";
    }
    
    /**
     * Endpoint para subscribe a atualizações gerais.
     * Usado pelo cliente para indicar que está pronto para receber notificações.
     */
    @MessageMapping("/subscribe")
    public void subscribe(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            System.out.println("Utilizador subscrito: " + auth.getName());
        }
    }
    
    /**
     * Endpoint para debug - retorna informações da sessão.
     */
    @MessageMapping("/info")
    @SendTo("/topic/info")
    public String getSessionInfo() {
        return "WebSocket Connection OK";
    }
}
