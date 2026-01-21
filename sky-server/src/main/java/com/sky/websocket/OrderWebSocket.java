package com.sky.websocket;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户端WebSocket服务
 * 用于向用户推送支付成功、订单状态变更等消息
 */
@Component
@Slf4j
@ServerEndpoint("/ws/order/{userId}")
public class OrderWebSocket {
    
    /**
     * 存储用户连接 userId -> Session
     * 使用ConcurrentHashMap保证线程安全
     */
    private static ConcurrentHashMap<Long, Session> userSessions = new ConcurrentHashMap<>();
    
    /**
     * 连接建立成功调用
     * @param session WebSocket会话
     * @param userId 用户ID
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        userSessions.put(userId, session);
        log.info("【WebSocket】用户{}建立连接，当前在线人数：{}", userId, userSessions.size());
        
        // 发送连接成功消息
        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.TYPE_CONNECTED)
                .message("连接成功")
                .timestamp(System.currentTimeMillis())
                .build();
        sendToUser(userId, message);
    }
    
    /**
     * 连接关闭调用
     * @param userId 用户ID
     */
    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        userSessions.remove(userId);
        log.info("【WebSocket】用户{}断开连接，当前在线人数：{}", userId, userSessions.size());
    }
    
    /**
     * 收到客户端消息
     * @param message 消息内容
     * @param userId 用户ID
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") Long userId) {
        log.info("【WebSocket】收到用户{}的消息：{}", userId, message);
        
        // 处理心跳
        if ("ping".equals(message)) {
            WebSocketMessage pong = WebSocketMessage.builder()
                    .type(WebSocketMessage.TYPE_PONG)
                    .timestamp(System.currentTimeMillis())
                    .build();
            sendToUser(userId, pong);
        }
    }
    
    /**
     * 发生错误
     * @param session WebSocket会话
     * @param error 错误信息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("【WebSocket】发生错误", error);
    }
    
    /**
     * 推送消息给指定用户
     * @param userId 用户ID
     * @param message 消息对象
     */
    public static void sendToUser(Long userId, WebSocketMessage message) {
        Session session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = JSON.toJSONString(message);
                session.getAsyncRemote().sendText(jsonMessage);
                log.info("【WebSocket】推送消息给用户{}：{}", userId, message.getType());
            } catch (Exception e) {
                log.error("【WebSocket】推送消息失败，用户ID：{}", userId, e);
            }
        } else {
            log.warn("【WebSocket】用户{}不在线，无法推送消息", userId);
        }
    }
    
    /**
     * 推送支付成功消息
     * @param userId 用户ID
     * @param orderNumber 订单号
     * @param orderData 订单数据
     */
    public static void sendPaymentSuccess(Long userId, String orderNumber, Object orderData) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.TYPE_PAYMENT_SUCCESS)
                .data(orderData)
                .message("支付成功")
                .timestamp(System.currentTimeMillis())
                .build();
        sendToUser(userId, message);
        log.info("【WebSocket】推送支付成功消息，用户ID：{}，订单号：{}", userId, orderNumber);
    }
    
    /**
     * 推送订单状态变更消息
     * @param userId 用户ID
     * @param orderData 订单数据
     */
    public static void sendOrderStatusChange(Long userId, Object orderData) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.TYPE_ORDER_STATUS)
                .data(orderData)
                .message("订单状态已更新")
                .timestamp(System.currentTimeMillis())
                .build();
        sendToUser(userId, message);
    }
    
    /**
     * 推送订单取消消息
     * @param userId 用户ID
     * @param orderData 订单数据
     */
    public static void sendOrderCancel(Long userId, Object orderData) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.TYPE_ORDER_CANCEL)
                .data(orderData)
                .message("订单已取消")
                .timestamp(System.currentTimeMillis())
                .build();
        sendToUser(userId, message);
    }
    
    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public static boolean isOnline(Long userId) {
        Session session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * 获取在线人数
     * @return 在线人数
     */
    public static int getOnlineCount() {
        return userSessions.size();
    }
}
