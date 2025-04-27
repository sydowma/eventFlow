package org.example.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class ThreadPerformanceTest {
    private static final int TASK_COUNT = 100; // 任务数量
    private static final int TASK_DURATION_MS = 1000; // 每个任务模拟的耗时（毫秒）

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting performance test with " + TASK_COUNT + " tasks...\n");

        // 测试虚拟线程
        testVirtualThreads();

        // 测试传统线程池
        testFixedThreadPool();
    }

    // 模拟任务：简单计算并休眠
   private static void simulateTask(int taskId) {
       try {
           // Simulate an HTTP call
           HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:8000")
                   .openConnection();
           connection.setRequestMethod("GET");
           connection.setConnectTimeout(TASK_DURATION_MS);
           connection.setReadTimeout(TASK_DURATION_MS);

           int responseCode = connection.getResponseCode();
           if (responseCode == 200) {
               // Simulate processing the response
               System.out.println("Task " + taskId + " completed successfully." + responseCode);
           }
       } catch (IOException e) {
           System.err.println("Task " + taskId + " failed: " + e.getMessage());
       }
   }

    // 测试虚拟线程性能
    private static void testVirtualThreads() {
        System.out.println("Testing Virtual Threads...");
        Runtime runtime = Runtime.getRuntime();
        long startMemory = runtime.totalMemory() - runtime.freeMemory();

        Instant start = Instant.now();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, TASK_COUNT)
                    .forEach(i -> executor.submit(() -> simulateTask(i)));
            // 等待所有任务完成
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Instant end = Instant.now();
        long endMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Virtual Threads: %d ms, Memory used: %d KB%n",
                Duration.between(start, end).toMillis(),
                (endMemory - startMemory) / 1024);
    }

    // 测试传统线程池性能
    private static void testFixedThreadPool() {
        System.out.println("\nTesting Fixed Thread Pool...");
        Runtime runtime = Runtime.getRuntime();
        long startMemory = runtime.totalMemory() - runtime.freeMemory();

        Instant start = Instant.now();
        try (var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)) {
            IntStream.range(0, TASK_COUNT)
                    .forEach(i -> executor.submit(() -> simulateTask(i)));
            // 等待所有任务完成
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Instant end = Instant.now();
        long endMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Fixed Thread Pool: %d ms, Memory used: %d KB%n",
                Duration.between(start, end).toMillis(),
                (endMemory - startMemory) / 1024);
    }
}
