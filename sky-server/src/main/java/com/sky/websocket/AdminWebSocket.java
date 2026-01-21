package com.sky.websocket;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商家端WebSocket服务
 * 用于向商家推送来单提醒、订单状态变更等消息
 */
@Component
@Slf4j
@ServerEndpoint("/ws/admin/{adminId}")
public class AdminWebSocket {
    
    /**
     * 存储商家连接 adminId -> Session
     */
    private static ConcurrentHashMap<Long, Session> adminSessions = new ConcurrentHashMap<>();
    
    /**
     * 连接建立成功调用
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("adminId") Long adminId) {
        adminSessions.put(adminId, session);
        log.info("【WebSocket-商家】商家{}建立连接，当前在线商家数：{}", adminId, adminSessions.size());
        
        // 发送连接成功消息
        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.TYPE_CONNECTED)
                .message("连接成功")
                .timestamp(System.currentTimeMillis())
                .build();
        sendToAdmin(adminId, message);
    }
    
    /**
     * 连接关闭调用
     */
    @OnClose
    public void onClose(@PathParam("adminId") Long adminId) {
        adminSessions.remove(adminId);
        log.info("【WebSocket-商家】商家{}断开连接，当前在线商家数：{}", adminId, adminSessions.size());
    }
    
    /**
     * 收到客户端消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("adminId") Long adminId) {
        log.info("【WebSocket-商家】收到商家{}的消息：{}", adminId, message);
        
        // 处理心跳
        if ("ping".equals(message)) {
            WebSocketMessage pong = WebSocketMessage.builder()
                    .type(WebSocketMessage.TYPE_PONG)
                    .timestamp(System.currentTimeMillis())
                    .build();
            sendToAdmin(adminId, pong);
        }
    }
    
    /**
     * 发生错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("【WebSocket-商家】发生错误", error);
    }
    
    /**
     * 推送消息给指定商家
     */
    public static void sendToAdmin(Long adminId, WebSocketMessage message) {
        Session session = adminSessions.get(adminId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = JSON.toJSONString(message);
                session.getAsyncRemote().sendText(jsonMessage);
                log.info("【WebSocket-商家】推送消息给商家{}：{}", adminId, message.getType());
            } catch (Exception e) {
                log.error("【WebSocket-商家】推送消息失败，商家ID：{}", adminId, e);
            }
        } else {
            log.warn("【WebSocket-商家】商家{}不在线，无法推送消息", adminId);
        }
    }
    
    /**
     * 推送来单提醒给所有在线商家
     * @param orderData 订单数据
     */
    public static void sendNewOrder(Object orderData) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.TYPE_NEW_ORDER)
                .data(orderData)
                .message("您有新的订单")
                .timestamp(System.currentTimeMillis())
                .build();
        
        String jsonMessage = JSON.toJSONString(message);
        
        // 广播给所有在线商家
        adminSessions.forEach((adminId, session) -> {
            if (session.isOpen()) {
                try {
                    session.getAsyncRemote().sendText(jsonMessage);
                    log.info("【WebSocket-商家】推送来单提醒给商家{}", adminId);
                } catch (Exception e) {
                    log.error("【WebSocket-商家】推送来单提醒失败，商家ID：{}", adminId, e);
                }
            }
        });
    }
    
    /**
     * 检查商家是否在线
     */
    public static boolean isOnline(Long adminId) {
        Session session = adminSessions.get(adminId);
        return session != null && session.isOpen();
    }
    
    /**
     * 获取在线商家数
     */
    public static int getOnlineCount() {
        return adminSessions.size();
    }
}
