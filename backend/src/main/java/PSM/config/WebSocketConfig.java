package PSM.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler; // <--- NOVO IMPORT
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration para notificações em tempo real via STOMP.
 * * Permite que o servidor envie notificações push aos clientes sem polling.
 * * Endpoints:
 * - ws://localhost:8080/ws (conexão WebSocket)
 * - /app/subscribe (enviar subscription)
 * - /user/queue/notifications (receber notificações personalizadas)
 * - /topic/updates (receber atualizações gerais)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. Criar e inicializar o agendador de tarefas necessário para gerir os pings (heartbeats)
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-thread-");
        scheduler.initialize();

        // 2. Ativa o message broker padrão (/topic e /queue) com o agendador e os teus 25s de heartbeat
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{25000, 25000})
              .setTaskScheduler(scheduler);
        
        // Prefixo para mensagens vindas do cliente para o servidor
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefixo para enviar notificações específicas do utilizador
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")  // Em produção, restringir domínios
                .withSockJS();  // Fallback para navegadores sem WebSocket
    }
}