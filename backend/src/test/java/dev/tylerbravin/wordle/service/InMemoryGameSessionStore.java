package dev.tylerbravin.wordle.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Plain in-memory {@link GameSessionStore} test double - no real Redis needed for unit tests. */
class InMemoryGameSessionStore implements GameSessionStore {

    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(GameSession session) {
        sessions.put(session.id(), session);
    }

    @Override
    public Optional<GameSession> find(UUID gameId) {
        return Optional.ofNullable(sessions.get(gameId));
    }
}
