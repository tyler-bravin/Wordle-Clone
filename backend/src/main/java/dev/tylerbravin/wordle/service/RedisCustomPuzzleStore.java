package dev.tylerbravin.wordle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed {@link CustomPuzzleStore}. The Redis TTL is derived from the
 * puzzle's own {@code expiresAt} (chosen by the creator, 1-48h - see
 * {@link CustomPuzzleService}) rather than a fixed default, so the link
 * actually stops resolving once it expires instead of just going stale.
 */
@Component
class RedisCustomPuzzleStore implements CustomPuzzleStore {

    private static final String KEY_PREFIX = "wordle:custom:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    RedisCustomPuzzleStore(StringRedisTemplate redis, ObjectMapper objectMapper, Clock clock) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void save(CustomPuzzle puzzle) {
        Duration ttl = Duration.between(clock.instant(), puzzle.expiresAt());
        redis.opsForValue().set(KEY_PREFIX + puzzle.id(), writeJson(puzzle), ttl);
    }

    @Override
    public Optional<CustomPuzzle> find(UUID puzzleId) {
        String json = redis.opsForValue().get(KEY_PREFIX + puzzleId);
        return json == null ? Optional.empty() : Optional.of(readJson(json));
    }

    private String writeJson(CustomPuzzle puzzle) {
        try {
            return objectMapper.writeValueAsString(puzzle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize custom puzzle " + puzzle.id(), e);
        }
    }

    private CustomPuzzle readJson(String json) {
        try {
            return objectMapper.readValue(json, CustomPuzzle.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize custom puzzle", e);
        }
    }
}
