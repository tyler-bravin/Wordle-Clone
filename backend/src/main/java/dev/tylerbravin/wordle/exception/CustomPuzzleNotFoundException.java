package dev.tylerbravin.wordle.exception;

import java.util.UUID;

public class CustomPuzzleNotFoundException extends RuntimeException {
    public CustomPuzzleNotFoundException(UUID puzzleId) {
        super("No custom puzzle found with id " + puzzleId);
    }
}
