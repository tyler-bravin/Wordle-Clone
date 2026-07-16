package dev.tylerbravin.wordle.service;

import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

/** Persists a player's Endless shuffle-bag order, keyed by {@code playerId}. */
interface EndlessBagStore {

    void save(UUID playerId, Deque<String> bag);

    Optional<Deque<String>> find(UUID playerId);

    boolean exists(UUID playerId);
}
