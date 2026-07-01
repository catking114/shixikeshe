package com.example.keshe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceWebSocketHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket client connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket client disconnected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received: {}", message.getPayload());
    }

    public void broadcastDeviceStatus(String eventType, Map<String, Object> data) {
        Map<String, Object> message = Map.of("type", eventType, "data", data, "timestamp", System.currentTimeMillis());
        try {
            String json = mapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("Send WS message failed", e);
                }
            });
        } catch (Exception e) {
            log.error("Serialize WS message failed", e);
        }
    }

    public int getClientCount() { return sessions.size(); }
}
