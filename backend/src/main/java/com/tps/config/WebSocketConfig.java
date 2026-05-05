package com.tps.config;

/**
 * 文件说明：WebSocket 配置类，负责 STOMP 端点、消息代理与连接鉴权。
 */

import com.tps.entity.User;
import com.tps.repository.UserRepository;
import com.tps.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 课程项目使用内存 broker 就够用，部署简单，真机联调时也方便直接起服务测试。
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Android 端实际连的是 SockJS 暴露出的 `/ws/websocket`，这里必须开启 SockJS 支持。
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // WebSocket 握手头不一定能被后续 STOMP 层直接复用，因此在 CONNECT 帧里再解析一次 JWT。
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    if (authorization == null) {
                        authorization = accessor.getFirstNativeHeader("authorization");
                    }
                    if (authorization != null && authorization.startsWith("Bearer ")) {
                        String token = authorization.substring(7);
                        if (jwtUtil.isTokenValid(token) && !"refresh".equals(jwtUtil.getType(token))) {
                            Long userId = jwtUtil.getUserId(token);
                            String role = jwtUtil.getRole(token);
                            userRepository.findById(userId).ifPresent(user -> {
                                if (user.getStatus() == User.UserStatus.ACTIVE) {
                                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                                            userId,
                                            null,
                                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                    ));
                                }
                            });
                        }
                    }
                }
                return message;
            }
        });
    }
}
