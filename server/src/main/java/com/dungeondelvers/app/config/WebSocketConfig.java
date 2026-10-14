package com.dungeondelvers.app.config;

import com.dungeondelvers.app.properties.DdoProperties;
import com.dungeondelvers.game.ws.GameWebSocketHandler;
import com.dungeondelvers.game.ws.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final DdoProperties ddoProperties;

    public WebSocketConfig(
            GameWebSocketHandler gameWebSocketHandler,
            JwtHandshakeInterceptor jwtHandshakeInterceptor,
            DdoProperties ddoProperties
    ) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.ddoProperties = ddoProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns(ddoProperties.cors().allowedOrigins());
    }
}
