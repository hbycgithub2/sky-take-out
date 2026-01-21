package com.sky.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * WebSocket消息实体
 * 统一的消息格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息类型
     */
    private String type;
    
    /**
     * 消息数据
     */
    private Object data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 消息内容描述
     */
    private String message;
    
    /**
     * 消息类型常量
     */
    public static final String TYPE_CONNECTED = "CONNECTED";                  // 连接成功
    public static final String TYPE_PAYMENT_SUCCESS = "PAYMENT_SUCCESS";      // 支付成功
    public static final String TYPE_ORDER_STATUS = "ORDER_STATUS_CHANGE";     // 订单状态变更
    public static final String TYPE_NEW_ORDER = "NEW_ORDER";                  // 新订单(商家端)
    public static final String TYPE_ORDER_CANCEL = "ORDER_CANCEL";            // 订单取消
    public static final String TYPE_PONG = "PONG";                            // 心跳响应
}
