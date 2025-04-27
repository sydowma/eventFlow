package org.example.api;

import com.example.event.LikeServiceGrpc;
// Import the generated Protobuf Event class
import com.example.event.EventOuterClass;
import com.google.common.util.concurrent.ListenableFuture;
// Import Futures and FutureCallback for bridging
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("")
public class EventController {

    // Use the non-blocking stub instead of blocking stub
    private final LikeServiceGrpc.LikeServiceStub likeServiceStub;
    private final LikeServiceGrpc.LikeServiceBlockingStub likeServiceBlockingStub;
    private final LikeServiceGrpc.LikeServiceFutureStub likeServiceFutureStub;

    @Autowired
    public EventController(
            LikeServiceGrpc.LikeServiceStub likeServiceStub,
            LikeServiceGrpc.LikeServiceBlockingStub likeServiceBlockingStub,
            LikeServiceGrpc.LikeServiceFutureStub likeServiceFutureStub) {
        this.likeServiceStub = likeServiceStub;
        this.likeServiceBlockingStub = likeServiceBlockingStub;
        this.likeServiceFutureStub = likeServiceFutureStub;
    }

    @PostMapping("/like")
    public CompletableFuture<String> like(@RequestBody Event event) {
//        System.out.println("Received like request: " + event);

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        EventOuterClass.Event protoEvent = convertToProto(event);

        // Initiate async gRPC server-streaming call
//        likeServiceStub.withDeadlineAfter(200, TimeUnit.MILLISECONDS)
//                .likeEvent(protoEvent, new StreamObserver<>() {
//                    @Override
//                    public void onNext(EventOuterClass.Event response) {
//                        // Process each streamed response
//                        responseFuture.complete("Like event processed successfully. Response ID: " + response.getId());
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//                        System.err.println("Error during gRPC call: " + throwable.getMessage());
//                        responseFuture.completeExceptionally(throwable);
//                    }
//
//                    @Override
//                    public void onCompleted() {
//                        if (!responseFuture.isDone()) {
//                            responseFuture.complete("Like event processed successfully, but no response received.");
//                        }
//                    }
//                });
//
//        return responseFuture;
        ListenableFuture<EventOuterClass.Event> future = likeServiceFutureStub.likeEvent(protoEvent);
        future.addListener(() -> {
            try {
                responseFuture.complete("Like event processed successfully. Response ID: " + future.get().getId());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }, MoreExecutors.directExecutor());
        return responseFuture;
    }
    
    @PostMapping("/batch-like")
    public CompletableFuture<List<String>> batchLike(@RequestBody List<Event> events) {
        CompletableFuture<List<String>> finalResult = new CompletableFuture<>();
        
        try {
            List<CompletableFuture<String>> futures = new ArrayList<>(events.size());
            
            for (Event event : events) {
                CompletableFuture<String> future = new CompletableFuture<>();
                futures.add(future);
                
                EventOuterClass.Event protoEvent = convertToProto(event);
                
                likeServiceStub.likeEvent(protoEvent, new StreamObserver<EventOuterClass.Event>() {
                    @Override
                    public void onNext(EventOuterClass.Event response) {
                        future.complete("Like event processed successfully. Response ID: " + response.getId());
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                    
                    @Override
                    public void onCompleted() {
                        if (!future.isDone()) {
                            future.complete("Like event processed, but no response received.");
                        }
                    }
                });
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    List<String> results = new ArrayList<>(events.size());
                    for (CompletableFuture<String> future : futures) {
                        try {
                            results.add(future.join());
                        } catch (Exception e) {
                            results.add("Error: " + e.getMessage());
                        }
                    }
                    finalResult.complete(results);
                })
                .exceptionally(ex -> {
                    finalResult.completeExceptionally(ex);
                    return null;
                });
        } catch (Exception e) {
            finalResult.completeExceptionally(e);
        }
        
        return finalResult;
    }

    private EventOuterClass.Event convertToProto(Event event) {
        return EventOuterClass.Event.newBuilder()
                .setId(event.id() != null ? event.id() : "")
                .setType(event.name() != null ? event.name() : "")
                .setData(event.description() != null ? event.description() : "")
                .setTimestamp(event.date() != null ? event.date() : "")
                .build();
    }

    @PostMapping("/unlike")
    public CompletableFuture<String> unlike(@RequestBody Event event) {
        System.out.println("Received unlike request: " + event);

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        try {
            EventOuterClass.Event protoEvent = EventOuterClass.Event.newBuilder()
                    .setId(event.id() != null ? event.id() : "")
                    .setTimestamp(event.date() != null ? event.date() : "")
                    .build();
            
            likeServiceStub.unlikeEvent(protoEvent, new StreamObserver<EventOuterClass.Event>() {
                @Override
                public void onNext(EventOuterClass.Event response) {
                    responseFuture.complete("Unlike event processed successfully. Response ID: " + response.getId());
                }
                
                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Error during gRPC call: " + throwable.getMessage());
                    responseFuture.completeExceptionally(throwable);
                }
                
                @Override
                public void onCompleted() {
                    if (!responseFuture.isDone()) {
                        responseFuture.complete("Unlike event processed successfully, but no response received.");
                    }
                }
            });
        } catch (Exception e) {
            responseFuture.completeExceptionally(e);
        }
        
        return responseFuture;
    }
    
    @PostMapping("/batch-unlike")
    public CompletableFuture<List<String>> batchUnlike(@RequestBody List<Event> events) {
        CompletableFuture<List<String>> finalResult = new CompletableFuture<>();
        
        try {
            List<CompletableFuture<String>> futures = new ArrayList<>(events.size());
            
            for (Event event : events) {
                CompletableFuture<String> future = new CompletableFuture<>();
                futures.add(future);
                
                EventOuterClass.Event protoEvent = EventOuterClass.Event.newBuilder()
                        .setId(event.id() != null ? event.id() : "")
                        .setTimestamp(event.date() != null ? event.date() : "")
                        .build();
                        
                likeServiceStub.unlikeEvent(protoEvent, new StreamObserver<EventOuterClass.Event>() {
                    @Override
                    public void onNext(EventOuterClass.Event response) {
                        future.complete("Unlike event processed successfully. Response ID: " + response.getId());
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                    
                    @Override
                    public void onCompleted() {
                        if (!future.isDone()) {
                            future.complete("Unlike event processed, but no response received.");
                        }
                    }
                });
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    List<String> results = new ArrayList<>(events.size());
                    for (CompletableFuture<String> future : futures) {
                        try {
                            results.add(future.join());
                        } catch (Exception e) {
                            results.add("Error: " + e.getMessage());
                        }
                    }
                    finalResult.complete(results);
                })
                .exceptionally(ex -> {
                    finalResult.completeExceptionally(ex);
                    return null;
                });
        } catch (Exception e) {
            finalResult.completeExceptionally(e);
        }
        
        return finalResult;
    }
}
