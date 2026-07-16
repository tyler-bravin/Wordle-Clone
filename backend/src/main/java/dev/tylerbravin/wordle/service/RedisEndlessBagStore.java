package dev.tylerbravin.wordle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tylerbravin.wordle.config.GameProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed {@link EndlessBagStore} so a player's shuffle-bag order (and with
 * it, the no-repeat guarantee) survives a backend restart instead of silently
 * resetting.
 */
@Component
class RedisEndlessBagStore implements EndlessBagStore {

    private static final String KEY_PREFIX = "wordle:bag:";
    private static final TypeReference<ArrayDeque<String>> BAG_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final GameProperties properties;

    RedisEndlessBagStore(StringRedisTemplate redis, ObjectMapper objectMapper, GameProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void save(UUID playerId, Deque<String> bag) {
        redis.opsForValue().set(KEY_PREFIX + playerId, writeJson(bag), properties.bagTtl());
    }

    @Override
    public Optional<Deque<String>> find(UUID playerId) {
        String json = redis.opsForValue().get(KEY_PREFIX + playerId);
        return json == null ? Optional.empty() : Optional.of(readJson(json));
    }

    @Override
    public boolean exists(UUID playerId) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + playerId));
    }

    private String writeJson(Deque<String> bag) {
        try {
            return objectMapper.writeValueAsString(bag);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize endless bag", e);
        }
    }

    private Deque<String> readJson(String json) {
        try {
            return objectMapper.readValue(json, BAG_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize endless bag", e);
        }
    }
}
