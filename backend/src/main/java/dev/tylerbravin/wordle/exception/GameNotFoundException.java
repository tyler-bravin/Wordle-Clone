package dev.tylerbravin.wordle.exception;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(UUID gameId) {
        super("No game found with id " + gameId);
    }
}
