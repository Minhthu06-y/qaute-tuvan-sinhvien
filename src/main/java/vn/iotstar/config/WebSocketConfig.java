package vn.iotstar.config;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import vn.iotstar.entity.User;
import vn.iotstar.enums.Role;
import vn.iotstar.enums.UserStatus;
import vn.iotstar.repository.UserRepository;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final UserRepository users;

    public WebSocketConfig(UserRepository users) {
        this.users = users;
    }

    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) return message;

                if (accessor.getCommand() == StompCommand.SEND) {
                    throw new AccessDeniedException("Client messages are not allowed");
                }
                if (accessor.getCommand() != StompCommand.SUBSCRIBE) return message;

                Principal principal = accessor.getUser();
                if (principal == null) throw new AccessDeniedException("Authentication is required");
                User user = users.findByEmailIgnoreCase(principal.getName())
                        .filter(candidate -> candidate.isEnabled() && candidate.getRole() != null
                                && candidate.getStatus() == UserStatus.ACTIVE)
                        .orElseThrow(() -> new AccessDeniedException("Account is not active"));
                authorizeSubscription(accessor.getDestination(), user);
                return message;
            }
        });
    }

    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    private void authorizeSubscription(String destination, User user) {
        if (destination == null) throw new AccessDeniedException("Subscription destination is required");
        if (destination.startsWith("/topic/notifications/")) {
            Long requestedUserId = parseId(destination.substring("/topic/notifications/".length()));
            if (!user.getId().equals(requestedUserId)) throw new AccessDeniedException("Not your notifications");
            return;
        }
        if (destination.startsWith("/topic/faculties/")) {
            Long requestedFacultyId = parseId(destination.substring("/topic/faculties/".length()));
            if (user.getRole() != Role.MANAGER || user.getFaculty() == null
                    || !user.getFaculty().getId().equals(requestedFacultyId)) {
                throw new AccessDeniedException("Not authorized for this faculty");
            }
            return;
        }
        if ("/topic/announcements".equals(destination)) return;
        throw new AccessDeniedException("Subscription destination is not allowed");
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Invalid subscription destination");
        }
    }
}
