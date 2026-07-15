package dev.tylerbravin.wordle.exception;

import java.util.UUID;

/**
 * Thrown when an Endless-mode request references a {@code playerId} that doesn't
 * correspond to a known shuffle bag - e.g. it expired, was never created, or the
 * server restarted (bags are in-memory only).
 */
public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(UUID playerId) {
        super("No endless session found for player " + playerId);
    }
}
