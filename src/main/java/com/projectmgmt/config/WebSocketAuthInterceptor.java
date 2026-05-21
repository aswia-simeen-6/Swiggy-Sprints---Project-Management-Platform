package com.projectmgmt.config;

import com.projectmgmt.common.util.JwtUtils;
import com.projectmgmt.domain.user.User;
import com.projectmgmt.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String token = authHeaders.get(0);
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }

                if (jwtUtils.isTokenValid(token)) {
                    UUID userId = jwtUtils.extractUserId(token);
                    User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
                    if (user != null) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        accessor.setUser(auth);
                        log.debug("WebSocket authenticated: {} ({})", user.getDisplayName(), userId);
                    }
                }
            }
        }
        return message;
    }
}
