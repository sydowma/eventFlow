package org.example.engine;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.example.engine.service.LikeServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.*;

@Component
public class GrpcServer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    @Value("${grpc.server.port:6565}")
    private int port;

    private Server server;

    @Autowired
    private LikeServiceImpl likeServiceImpl; // Inject the service implementation

    @Override
    public void run(ApplicationArguments args) throws Exception {
        start();
        blockUntilShutdown();
    }

    public void start() throws IOException {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
//        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        server = ServerBuilder.forPort(port)
                .addService(likeServiceImpl) // Register our service
                .executor(executorService)
                .build()
                .start();
        log.info("gRPC Server started, listening on port {}", port);

        // Add a shutdown hook to gracefully stop the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("*** shutting down gRPC server since JVM is shutting down");
            try {
                GrpcServer.this.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("gRPC server shutdown interrupted", e);
            }
            log.info("*** server shut down");
        }));
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
