package org.example.engine.service;

import com.example.event.EventOuterClass;
import com.example.event.LikeServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        // TODO: Implement actual like logic here
        // For now, just echoing the request back with a modified type

        EventOuterClass.Event response = request.toBuilder()
                .setType("LIKED_EVENT") // Indicate the event was liked
                .setData("Event " + request.getId() + " liked successfully.")
                .build();

        eventStore.putIfAbsent(request.getId(), 0L);
        eventStore.computeIfPresent(response.getId(), (k, v) -> v + 1);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
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