package dev.tylerbravin.wordle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tylerbravin.wordle.config.GameProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed {@link GameSessionStore} so a game session survives a backend
 * restart, and (were this ever run with more than one instance) is shared across
 * them rather than living on a single instance's heap.
 */
@Component
class RedisGameSessionStore implements GameSessionStore {

    private static final String KEY_PREFIX = "wordle:game:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final GameProperties properties;

    RedisGameSessionStore(StringRedisTemplate redis, ObjectMapper objectMapper, GameProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void save(GameSession session) {
        redis.opsForValue().set(KEY_PREFIX + session.id(), writeJson(session), properties.sessionTtl());
    }

    @Override
    public Optional<GameSession> find(UUID gameId) {
        String json = redis.opsForValue().get(KEY_PREFIX + gameId);
        return json == null ? Optional.empty() : Optional.of(readJson(json));
    }

    private String writeJson(GameSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize game session " + session.id(), e);
        }
    }

    private GameSession readJson(String json) {
        try {
            return objectMapper.readValue(json, GameSession.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize game session", e);
        }
    }
}
