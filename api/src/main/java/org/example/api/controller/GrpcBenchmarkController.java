package org.example.api.controller;

import org.example.api.client.GrpcStreamingClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
// It's good practice to also import Logger and LoggerFactory if you plan to add logging
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/benchmark")
public class GrpcBenchmarkController {

    // private static final Logger log = LoggerFactory.getLogger(GrpcBenchmarkController.class);

    // Consider making host and port configurable via application properties
    private static final String GRPC_HOST = "localhost"; 
    // Ensure this is the port your gRPC service in 'engine' module runs on.
    // The default gRPC port is often 9090 if not specified otherwise in the server.
    private static final int GRPC_PORT = 9090; 

    @GetMapping("/grpc/stream")
    public Map<String, Object> benchmarkGrpcStream(
            @RequestParam(defaultValue = "1000") int numEvents) {
        
        GrpcStreamingClient client = new GrpcStreamingClient(GRPC_HOST, GRPC_PORT);
        Map<String, Object> response = new HashMap<>();
        long startTime = 0;
        long endTime = 0;
        String streamResult = "";

        try {
            startTime = System.nanoTime();
            streamResult = client.streamEvents(numEvents); // This method can throw InterruptedException
            endTime = System.nanoTime();

            response.put("status", "success");
            response.put("grpc_stream_result", streamResult); // Consistent key name
            response.put("events_configured", numEvents); // Clarify this is the number requested
            long durationNanos = endTime - startTime;
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos);
            response.put("duration_millis", durationMillis);
            
            if (durationMillis > 0) {
                double eventsPerSecond = (double) numEvents / durationMillis * 1000.0;
                response.put("events_per_second", String.format("%.2f", eventsPerSecond));
            } else if (durationNanos > 0) { // Handle very short durations accurately
                 double eventsPerSecond = (double) numEvents / durationNanos * 1_000_000_000.0;
                 response.put("events_per_second", String.format("%.2f", eventsPerSecond) + " (duration < 1ms)");
            }
            else {
                response.put("events_per_second", "N/A (duration too short or zero)");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            response.put("status", "error");
            response.put("message", "Benchmark interrupted: " + e.getMessage());
            if (startTime > 0 && endTime == 0) endTime = System.nanoTime(); 
        } catch (Exception e) {
            // Catching a broader range of exceptions from client.streamEvents() or client creation
            response.put("status", "error");
            response.put("message", "Exception during benchmark: " + e.getClass().getName() + " - " + e.getMessage());
            if (startTime > 0 && endTime == 0) endTime = System.nanoTime();
        } finally {
            try {
                // log.info("Shutting down GrpcStreamingClient from controller.");
                client.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                // log.warn("Interrupted while shutting down gRPC client.", e);
                response.put("client_shutdown_status", "interrupted");
                response.put("client_shutdown_error", e.getMessage());
            } catch (Exception e) { // Catch other potential shutdown errors
                // log.error("Error shutting down gRPC client.", e);
                response.put("client_shutdown_status", "error");
                response.put("client_shutdown_error", e.getClass().getName() + " - " + e.getMessage());
            }
        }
        
        // Add duration to response even if there was an error, if times were captured
        if (startTime > 0 && endTime > 0 && !response.containsKey("duration_millis")) {
             response.put("duration_millis", TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
        }
        return response;
    }
}
