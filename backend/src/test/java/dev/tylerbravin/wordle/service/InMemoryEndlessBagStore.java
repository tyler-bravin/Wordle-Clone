package dev.tylerbravin.wordle.service;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Plain in-memory {@link EndlessBagStore} test double - no real Redis needed for unit tests. */
class InMemoryEndlessBagStore implements EndlessBagStore {

    private final Map<UUID, Deque<String>> bags = new ConcurrentHashMap<>();

    @Override
    public void save(UUID playerId, Deque<String> bag) {
        bags.put(playerId, bag);
    }

    @Override
    public Optional<Deque<String>> find(UUID playerId) {
        return Optional.ofNullable(bags.get(playerId));
    }

    @Override
    public boolean exists(UUID playerId) {
        return bags.containsKey(playerId);
    }
}
