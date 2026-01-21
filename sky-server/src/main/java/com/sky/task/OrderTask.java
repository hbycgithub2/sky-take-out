package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.websocket.OrderWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单定时任务
 * 用于处理超时订单等定时任务
 */
@Component
@Slf4j
public class OrderTask {
    
    @Autowired
    private OrderMapper orderMapper;
    
    /**
     * 处理超时订单
     * 每分钟执行一次，检查并取消15分钟未支付的订单
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟的第0秒执行
    public void processTimeoutOrder() {
        log.info("========== 【定时任务】开始处理超时订单 ==========");
        
        // 查询15分钟前创建的待付款订单
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        
        // 查询超时订单
        List<Orders> timeoutOrders = orderMapper.getByStatusAndOrderTimeLT(
            Orders.PENDING_PAYMENT, time);
        
        if (timeoutOrders != null && timeoutOrders.size() > 0) {
            log.info("【定时任务】发现{}个超时订单", timeoutOrders.size());
            
            for (Orders order : timeoutOrders) {
                // 更新订单状态为已取消
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
                
                log.info("【定时任务】订单{}已取消，订单号：{}", order.getId(), order.getNumber());
                
                // 推送WebSocket消息给用户
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("orderId", order.getId());
                orderData.put("orderNumber", order.getNumber());
                orderData.put("cancelReason", "订单超时，自动取消");
                
                OrderWebSocket.sendOrderCancel(order.getUserId(), orderData);
                log.info("【定时任务】已推送取消通知给用户，用户ID：{}", order.getUserId());
            }
            
            log.info("【定时任务】处理超时订单完成，共处理{}个订单", timeoutOrders.size());
        } else {
            log.info("【定时任务】没有超时订单");
        }
        
        log.info("========== 【定时任务】处理超时订单结束 ==========");
    }
}
