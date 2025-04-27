package org.example.engine.service;

import com.example.event.EventOuterClass;
import com.example.event.LikeServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service // Annotate as a Spring service
public class LikeServiceImpl extends LikeServiceGrpc.LikeServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(LikeServiceImpl.class);

    private Map<String, Long> eventStore = new ConcurrentHashMap<>();

    @Override
    public void likeEvent(EventOuterClass.Event request, StreamObserver<EventOuterClass.Event> responseObserver) {

        request.getTypeBytes().asReadOnlyByteBuffer();
        try {
            // Validate request
            eventStore.putIfAbsent(request.getId(), 0L);
            Long newCount = eventStore.computeIfPresent(request.getId(), (k, v) -> v + 1);

            // Build response
            EventOuterClass.Event response = request.toBuilder()
                    .setType("LIKED_EVENT")
                    .setData("Event liked successfully. Current likes: " + newCount)
                    .build();

            // Send response and complete the stream
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to process like: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void unlikeEvent(EventOuterClass.Event request, StreamObserver<EventOuterClass.Event> responseObserver) {

        // TODO: Implement actual unlike logic here
        // For now, just echoing the request back with a modified type

        System.out.println("Received unlikeEvent request for event ID: " + request.getId());

        EventOuterClass.Event response = request.toBuilder()
                .setType("UNLIKED_EVENT") // Indicate the event was unliked
                .setData("Event " + request.getId() + " unliked successfully.")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
} 