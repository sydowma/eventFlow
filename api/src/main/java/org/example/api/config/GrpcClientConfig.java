package org.example.api.config;

import com.example.event.LikeServiceGrpc;
import io.grpc.CompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;

@Configuration
public class GrpcClientConfig {

    // Inject the gRPC server address and port from application properties
    // Example: grpc.server.address=localhost
    // Example: grpc.server.port=6565
    // TODO: Add these properties to your application.properties or application.yml
    @Value("${grpc.server.address:localhost}")
    private String grpcServerAddress;

    @Value("${grpc.server.port:6565}")
    private int grpcServerPort;
    
    private final int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;

//    @Value("${grpc.client.max-inbound-message-size:131072}") // 128K
    private final int maxInboundMessageSize = 131072;

    @Value("${grpc.client.deadline-seconds:5}")
    private int deadlineSeconds;
    
    private ManagedChannel channel;

    @Bean
    public ManagedChannel managedChannel() {
        // Create an optimized thread pool with proper sizing

        // Use VirtualThreadExecutor for better performance with gRPC

        // increase TPS 10000
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
//        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        // Create a communication channel to the gRPC server with optimized settings
        channel = ManagedChannelBuilder.forAddress(grpcServerAddress, grpcServerPort)
                // Connection pooling optimization
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(20, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                // Performance optimizations
                .maxInboundMessageSize(maxInboundMessageSize)
                .maxRetryAttempts(0)                   // Disable retries to avoid backpressure
                .compressorRegistry(CompressorRegistry.getDefaultInstance())
                // Use the optimized thread pool
                .executor(executorService)
                .build();
                
        return channel;
    }

    @Bean
    public LikeServiceGrpc.LikeServiceBlockingStub likeServiceBlockingStub(ManagedChannel channel) {
        // Increased deadline to avoid premature timeouts
        return LikeServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS);
    }

    // Optionally, you can also provide an async stub bean if needed
    // Inject this stub for non-blocking calls
    @Bean
    public LikeServiceGrpc.LikeServiceStub likeServiceStub(ManagedChannel channel) {
        // Increased deadline to avoid premature timeouts
        return LikeServiceGrpc.newStub(channel)
                .withCompression("gzip")
                .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS);
    }

    @Bean
    public LikeServiceGrpc.LikeServiceFutureStub likeServiceFutureStub(ManagedChannel channel) {
        return LikeServiceGrpc.newFutureStub(channel);
    }

    
    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (!channel.isTerminated()) {
                    channel.shutdownNow();
                }
            }
        }
    }
} 