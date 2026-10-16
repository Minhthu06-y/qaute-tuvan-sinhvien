package vn.iotstar.qaute.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Chung cả nhóm - nếu Người 2 đã có thì giữ 1 bản duy nhất.
 * Principal của WebSocket phải trả về EMAIL (xác thực JWT lúc handshake/CONNECT do Người 1 làm)
 * để convertAndSendToUser(email, ...) gửi đúng người.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override public void configureMessageBroker(MessageBrokerRegistry r) {
        r.enableSimpleBroker("/topic", "/queue");
        r.setApplicationDestinationPrefixes("/app");
        r.setUserDestinationPrefix("/user");
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry r) {
        r.addEndpoint("/ws").withSockJS();
    }
}

