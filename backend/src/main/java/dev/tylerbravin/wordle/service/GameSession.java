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
 */
class GameSession {

    private final UUID id;
    private final GameMode mode;
    /** Calendar day index for DAILY games, or shuffle-bag position for ENDLESS games. */
    private final long roundNumber;
    private final String answer;
    private final int maxGuesses;
    private final List<GuessResult> guesses = new ArrayList<>();
    private GameStatus status = GameStatus.IN_PROGRESS;

    GameSession(UUID id, GameMode mode, long roundNumber, String answer, int maxGuesses) {
        this.id = id;
        this.mode = mode;
        this.roundNumber = roundNumber;
        this.answer = answer;
        this.maxGuesses = maxGuesses;
    }

    UUID id() {
        return id;
    }

    GameMode mode() {
        return mode;
    }

    long roundNumber() {
        return roundNumber;
    }

    String answer() {
        return answer;
    }

    int maxGuesses() {
        return maxGuesses;
    }

    List<GuessResult> guesses() {
        return guesses;
    }

    GameStatus status() {
        return status;
    }

    void addGuess(GuessResult result) {
        guesses.add(result);
    }

    void setStatus(GameStatus status) {
        this.status = status;
    }

    boolean isFinished() {
        return status != GameStatus.IN_PROGRESS;
    }
}
