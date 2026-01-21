package com.sky.controller.user;

 

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class Orders {
    // 订单ID
    private final String OrdersId;
    // 订单状态
    private String status;
    // 订单金额
    private final double amount;

    // 构造函数，初始化订单ID和金额，设置订单状态为CREATED
    public Orders(String OrdersId, double amount) {
        this.OrdersId = OrdersId;
        this.amount = amount;
        this.status = "CREATED";
    }

    // 模拟订单处理流程，设置订单状态为PAID和SHIPPED
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

    // 重写toString方法，返回订单ID、金额和状态
    @Override
    public String toString() {
        return "OrdersId:" + OrdersId + " amount:" + amount + " status:" + status + ":  " + Thread.currentThread().getName();
    }
}

public class OrdersProcessingSystems {
    private static final int THREAD_POOL_SIZE = 5;
    private static final int Orders_COUNT = 100;

    public static void main(String[] args) {
        // 创建包含5个线程的固定线程池
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Orders>> futures = new ArrayList<>();

        // 提交100个订单处理任务
        for (int i = 1; i <= Orders_COUNT; i++) {
            final String OrdersId = "Orders-" + String.format("%03d", i);
            final double amount = 100.0 * i;

            futures.add(executor.submit(() -> {
                Orders Orders = new Orders(OrdersId, amount);
                Orders.process();
                return Orders;
            }));
        }

        // 按任务提交顺序合并处理结果
        List<Orders> processedOrderss = new ArrayList<>();
        for (Future<Orders> future : futures) {
            try {
                // 这里会按任务提交顺序获取结果
                processedOrderss.add(future.get());
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
        System.out.println("已处理订单列表（共" + processedOrderss.size() + "单）：");
        processedOrderss.forEach(System.out::println);
    }
}