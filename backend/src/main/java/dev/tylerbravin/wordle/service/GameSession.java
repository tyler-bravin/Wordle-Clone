package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.dto.GameMode;
import dev.tylerbravin.wordle.dto.GameStatus;
import dev.tylerbravin.wordle.dto.GuessResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side record of a single game in progress, for either {@link GameMode}.
 * The answer is never exposed to the client until the game ends (see
 * {@link GameService#toResponse}), so it can't be inferred by inspecting responses
 * mid-game.
 * <p>
 * Immutable, and round-trips through {@link GameSessionStore} as JSON - every state
 * change produces a new instance that must be explicitly saved back, rather than
 * being mutated in place, since a Redis-backed store has no notion of a live shared
 * object to mutate.
 */
record GameSession(
        UUID id,
        GameMode mode,
        /** Calendar day index for DAILY games, or shuffle-bag position for ENDLESS games. */
        long roundNumber,
        String answer,
        int maxGuesses,
        List<GuessResult> guesses,
        GameStatus status
) {

    static GameSession start(UUID id, GameMode mode, long roundNumber, String answer, int maxGuesses) {
        return new GameSession(id, mode, roundNumber, answer, maxGuesses, List.of(), GameStatus.IN_PROGRESS);
    }

    GameSession withGuess(GuessResult result, GameStatus newStatus) {
        List<GuessResult> updated = new ArrayList<>(guesses);
        updated.add(result);
        return new GameSession(id, mode, roundNumber, answer, maxGuesses, List.copyOf(updated), newStatus);
    }

    boolean isFinished() {
        return status != GameStatus.IN_PROGRESS;
    }
}
