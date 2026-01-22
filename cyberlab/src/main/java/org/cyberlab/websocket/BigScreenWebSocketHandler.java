package org.cyberlab.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cyberlab.service.BigScreenService;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class BigScreenWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(BigScreenWebSocketHandler.class);

    private final BigScreenService bigScreenService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BigScreenWebSocketHandler(BigScreenService bigScreenService) {
        this.bigScreenService = bigScreenService;
        this.objectMapper = new ObjectMapper();
        startDataPushScheduler();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        logger.info("WebSocket连接建立: {}, 当前连接数: {}", session.getId(), sessions.size());

        sendInitialData(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        logger.info("WebSocket连接关闭: {}, 当前连接数: {}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("收到WebSocket消息: {}", message.getPayload());
    }

    private void sendInitialData(WebSocketSession session) {
        try {
            Map<String, Object> data = bigScreenService.getPublicBigScreenData();
            String jsonData = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(jsonData));
        } catch (Exception e) {
            logger.error("发送初始数据失败", e);
        }
    }

    private void startDataPushScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            if (sessions.isEmpty()) {
                return;
            }

            try {
                Map<String, Object> data = bigScreenService.getPublicBigScreenData();
                String jsonData = objectMapper.writeValueAsString(data);
                TextMessage message = new TextMessage(jsonData);

                sessions.values().forEach(session -> {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(message);
                        } catch (IOException e) {
                            logger.error("推送数据到会话{}失败", session.getId(), e);
                        }
                    }
                });

                logger.debug("推送大屏数据到{}个活跃连接", sessions.size());
            } catch (Exception e) {
                logger.error("获取大屏数据失败", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
