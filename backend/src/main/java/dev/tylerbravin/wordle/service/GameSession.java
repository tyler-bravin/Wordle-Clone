package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.dto.GameStatus;
import dev.tylerbravin.wordle.dto.GuessResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side record of a single game in progress. The answer is never sent to the
 * client until the game ends, so guesses can't be inferred by inspecting responses.
 */
class GameSession {

    private final UUID id;
    private final LocalDate date;
    private final long dayNumber;
    private final String answer;
    private final int maxGuesses;
    private final List<GuessResult> guesses = new ArrayList<>();
    private GameStatus status = GameStatus.IN_PROGRESS;

    GameSession(UUID id, LocalDate date, long dayNumber, String answer, int maxGuesses) {
        this.id = id;
        this.date = date;
        this.dayNumber = dayNumber;
        this.answer = answer;
        this.maxGuesses = maxGuesses;
    }

    UUID id() {
        return id;
    }

    LocalDate date() {
        return date;
    }

    long dayNumber() {
        return dayNumber;
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
