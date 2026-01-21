package com.sky.controller.user;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MultiThreadMerge {
    public static void main(String[] args) {
        // 创建固定大小的线程池
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<String>> futures = new ArrayList<>();

        // 提交任务到线程池
        for (int i = 1; i <= 5; i++) {
            int taskId = i;
            futures.add(executor.submit(() -> processTask(taskId)));
        }

        // 合并结果
        StringBuilder finalResult = new StringBuilder();
        for (Future<String> future : futures) {
            try {
                finalResult.append(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // 关闭线程池
        executor.shutdown();

        System.out.println("Final Merged Result:\n" + finalResult);
    }

    private static String processTask(int taskId) {
        try {
            // 模拟任务处理耗时
            Thread.sleep(1000);
            return "Task " + taskId + " processed by " + Thread.currentThread().getName() + "\n";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }
}