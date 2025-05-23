package org.example.api;

import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.redisson.api.RedissonClient; // Added import
import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam; // Added import
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap; // Added import
import java.util.Map; // Added import
import java.util.concurrent.TimeUnit; // Added import

@RestController
public class EventRedisController {

    private final RedisStringCommands<String, String> redisStringCommands;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient; // Added field

    // Updated constructor to include RedissonClient
    public EventRedisController(RedisStringCommands<String, String> redisStringCommands,
                                StringRedisTemplate stringRedisTemplate,
                                RedissonClient redissonClient) {
        this.redisStringCommands = redisStringCommands;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient; // Initialize new field
    }

    @PostMapping(value = "/redis/like", consumes = "application/json")
    public String postEventForRedis(@RequestBody Event event) {

        SetArgs setArgs = new SetArgs();
        setArgs.ex(1000);
        return redisStringCommands.set("event:" + event.id(), event.date(), setArgs);

    }

    @GetMapping("/redis/stream/benchmark")
    public Map<String, Object> benchmarkRedisStream(
            @RequestParam(defaultValue = "1000") int numEvents) {

        Map<String, Object> response = new HashMap<>();
        long startTime = 0;
        long endTime = 0;
        SetArgs setArgs = SetArgs.Builder.ex(60); // Expire keys after 60 seconds

        try {
            startTime = System.nanoTime();
            for (int i = 0; i < numEvents; i++) {
                String key = "event:stream:" + System.currentTimeMillis() + ":" + i;
                String value = "data:" + i;
                // Make sure redisStringCommands is available and injected, it is in the constructor
                redisStringCommands.set(key, value, setArgs);
            }
            endTime = System.nanoTime();

            response.put("status", "success");
            response.put("events_sent", numEvents);
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            response.put("duration_millis", durationMillis);
            if (durationMillis > 0) {
                response.put("events_per_second", (numEvents / (double) durationMillis) * 1000.0);
            } else {
                response.put("events_per_second", "N/A (duration too short)");
            }

        } catch (Exception e) {
            if (startTime > 0 && endTime == 0) endTime = System.nanoTime();
            response.put("status", "error");
            response.put("message", "Exception during Redis benchmark: " + e.getMessage());
        }
        
        if (startTime > 0 && endTime > 0 && !response.containsKey("duration_millis")) {
            response.put("duration_millis", TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
        }
        return response;
    }

    @GetMapping("/redisson/stream/benchmark")
    public Map<String, Object> benchmarkRedissonStream(
            @RequestParam(defaultValue = "1000") int numEvents) {
        
        Map<String, Object> response = new HashMap<>();
        long startTime = 0;
        long endTime = 0;

        try {
            startTime = System.nanoTime();
            for (int i = 0; i < numEvents; i++) {
                String key = "event:rd-stream:" + System.currentTimeMillis() + ":" + i;
                String value = "data:" + i;
                redissonClient.getBucket(key).set(value, 60, TimeUnit.SECONDS);
            }
            endTime = System.nanoTime();

            response.put("status", "success");
            response.put("events_sent", numEvents);
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            response.put("duration_millis", durationMillis);
            if (durationMillis > 0) {
                response.put("events_per_second", (numEvents / (double) durationMillis) * 1000.0);
            } else {
                response.put("events_per_second", "N/A (duration too short)");
            }

        } catch (Exception e) {
            if (startTime > 0 && endTime == 0) endTime = System.nanoTime();
            response.put("status", "error");
            response.put("message", "Exception during Redisson benchmark: " + e.getMessage());
        }
        
        if (startTime > 0 && endTime > 0 && !response.containsKey("duration_millis")) {
            response.put("duration_millis", TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
        }
        return response;
    }

    @GetMapping("/redis-template/stream/benchmark")
    public Map<String, Object> benchmarkRedisTemplateStream(
            @RequestParam(defaultValue = "1000") int numEvents) {
        
        Map<String, Object> response = new HashMap<>();
        long startTime = 0;
        long endTime = 0;

        try {
            startTime = System.nanoTime();
            for (int i = 0; i < numEvents; i++) {
                String key = "event:rt-stream:" + System.currentTimeMillis() + ":" + i;
                String value = "data:" + i;
                stringRedisTemplate.opsForValue().set(key, value, 60, TimeUnit.SECONDS);
            }
            endTime = System.nanoTime();

            response.put("status", "success");
            response.put("events_sent", numEvents);
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            response.put("duration_millis", durationMillis);
            if (durationMillis > 0) {
                response.put("events_per_second", (numEvents / (double) durationMillis) * 1000.0);
            } else {
                response.put("events_per_second", "N/A (duration too short)");
            }

        } catch (Exception e) {
            if (startTime > 0 && endTime == 0) endTime = System.nanoTime();
            response.put("status", "error");
            response.put("message", "Exception during RedisTemplate benchmark: " + e.getMessage());
        }
        
        if (startTime > 0 && endTime > 0 && !response.containsKey("duration_millis")) {
            response.put("duration_millis", TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
        }
        return response;
    }
}
