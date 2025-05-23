package org.example.api.client;

import com.example.event.EventOuterClass;
import com.example.event.LikeServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong; // Added for receivedEvents count
import org.slf4j.Logger; // Added for logging
import org.slf4j.LoggerFactory; // Added for logging

public class GrpcStreamingClient {
    private static final Logger log = LoggerFactory.getLogger(GrpcStreamingClient.class); // Added logger
    private final LikeServiceGrpc.LikeServiceStub streamingStub;
    private final ManagedChannel channel;

    public GrpcStreamingClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                                         .usePlaintext() // For simplicity, remove for production
                                         .build();
        this.streamingStub = LikeServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        log.info("Shutting down gRPC channel...");
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        log.info("Channel shutdown complete.");
    }

    public String streamEvents(int numEventsToSend) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicLong sentEvents = new AtomicLong(0); // For counting sent events
        final AtomicLong receivedEvents = new AtomicLong(0); // For counting received events

        StreamObserver<EventOuterClass.Event> responseObserver = new StreamObserver<EventOuterClass.Event>() {
            @Override
            public void onNext(EventOuterClass.Event event) {
                log.info("Client received event: ID={}, Type={}, Data={}", event.getId(), event.getType(), event.getData());
                receivedEvents.incrementAndGet();
            }

            @Override
            public void onError(Throwable t) {
                log.error("Client stream error: {}", t.getMessage(), t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Client stream completed by server.");
                finishLatch.countDown();
            }
        };

        StreamObserver<EventOuterClass.Event> requestObserver = streamingStub.likeEvent(responseObserver);

        try {
            log.info("Client starting to send {} events...", numEventsToSend);
            for (int i = 0; i < numEventsToSend; i++) {
                String eventId = "event-" + System.currentTimeMillis() + "-" + i;
                EventOuterClass.Event event = EventOuterClass.Event.newBuilder()
                        .setId(eventId)
                        .setType("TEST_EVENT")
                        .setData("Test data " + i)
                        .setTimestamp(String.valueOf(System.currentTimeMillis()))
                        .build();
                requestObserver.onNext(event);
                sentEvents.incrementAndGet();
                log.debug("Client sent event: ID={}", eventId);
                // Optional: add a small delay if needed for testing, e.g. Thread.sleep(10);
            }
        } catch (RuntimeException e) {
            log.error("Error sending events from client", e);
            requestObserver.onError(e); // Notify the server about the error
            finishLatch.countDown(); // Ensure latch is counted down on client-side error too
            throw e;
        }
        requestObserver.onCompleted();
        log.info("Client finished sending all events.");

        boolean completedInTime = finishLatch.await(1, TimeUnit.MINUTES);
        if (completedInTime) {
            return "Stream completed successfully. Sent: " + sentEvents.get() + ", Received: " + receivedEvents.get();
        } else {
            log.warn("Stream timed out after 1 minute.");
            return "Stream timed out. Sent: " + sentEvents.get() + ", Received: " + receivedEvents.get();
        }
    }
}
