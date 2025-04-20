package org.example.api.config;

import com.example.event.LikeServiceGrpc;
import io.grpc.CompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;

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
    
    @Value("${grpc.client.thread-pool-size:16}")
    private int threadPoolSize;
    
    @Value("${grpc.client.max-inbound-message-size:4194304}") // 4MB default
    private int maxInboundMessageSize;
    
    @Value("${grpc.client.max-outbound-message-size:4194304}") // 4MB default
    private int maxOutboundMessageSize;
    
    @Value("${grpc.client.deadline-seconds:60}")
    private int deadlineSeconds;
    
    private ManagedChannel channel;

    @Bean
    public ManagedChannel managedChannel() {
        // Create an optimized thread pool with proper sizing
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize);
        
        // Create a communication channel to the gRPC server with optimized settings
        channel = ManagedChannelBuilder.forAddress(grpcServerAddress, grpcServerPort)
                .usePlaintext() // For production, use TLS
                // Connection pooling optimization
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)           // Keep connection alive even without active calls
                // Performance optimizations
                .maxInboundMessageSize(maxInboundMessageSize)
                .maxRetryAttempts(0)                   // Disable retries to avoid backpressure
                .compressorRegistry(CompressorRegistry.getDefaultInstance())
                // Use the optimized thread pool
                .executor(executor)
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
                .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS);
    }

    @Bean
    public LikeServiceGrpc.LikeServiceFutureStub likeServiceFutureStub(ManagedChannel channel) {
        return LikeServiceGrpc.newFutureStub(channel);
    }

    // Define a separate executor for handling gRPC future callbacks
    @Bean(name = "grpcCallbackExecutor")
    public Executor grpcCallbackExecutor() {
        // Configure this pool based on expected callback load
        return Executors.newFixedThreadPool(threadPoolSize > 0 ? threadPoolSize : 10); 
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