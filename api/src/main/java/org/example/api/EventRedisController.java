package org.example.api;

import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventRedisController {

    private final RedisStringCommands<String, String> redisStringCommands;

    public EventRedisController(RedisStringCommands<String, String> redisStringCommands) {
        this.redisStringCommands = redisStringCommands;
    }

    @PostMapping(value = "/redis/like", consumes = "application/json")
    public void postEventForRedis(@RequestBody Event event) {

        System.out.println("Received event for Redis: " + event);
        SetArgs setArgs = new SetArgs();
        setArgs.ex(1000);
        redisStringCommands.set("event:" + event.id(), event.date(), setArgs);

    }
}
