package dev.tylerbravin.wordle.service;

import java.util.Optional;
import java.util.UUID;

/** Persists {@link GameSession} state, keyed by {@code gameId}. */
interface GameSessionStore {

    void save(GameSession session);

    Optional<GameSession> find(UUID gameId);
}
