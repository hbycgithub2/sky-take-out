package com.sky.controller.user;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class Order {
    private final String orderId;
    private String status;
    private final double amount;

    public Order(String orderId, double amount) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = "CREATED";
    }

    public void process() {
        try {
            // 模拟订单处理流程
            Thread.sleep(500);
            this.status = "PAID";
            Thread.sleep(500);
            this.status = "SHIPPED";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return "orderId:"+orderId+" amount:"+ amount+" status:"+ status+":  "+Thread.currentThread().getName();
    }
}

public class OrderProcessingSystem {
    private static final int THREAD_POOL_SIZE = 5;

    public static void main(String[] args) {
        // 创建包含5个线程的固定线程池
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Order>> futures = new ArrayList<>();

        // 提交5个订单处理任务
        for (int i = 1; i <= 5; i++) {
            final String orderId = "ORDER-" + String.format("%03d", i);
            final double amount = 100.0 * i;

            futures.add(executor.submit(() -> {
                Order order = new Order(orderId, amount);
                order.process();
                return order;
            }));
        }

        // 合并处理结果
        List<Order> processedOrders = new ArrayList<>();
        for (Future<Order> future : futures) {
            try {
                processedOrders.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("订单处理异常: " + e.getMessage());
            }
        }

        // 优雅关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 输出处理结果
        System.out.println("已处理订单列表：");
        processedOrders.forEach(System.out::println);
    }
}
