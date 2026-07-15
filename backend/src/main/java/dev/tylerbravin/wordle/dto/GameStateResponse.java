package dev.tylerbravin.wordle.dto;

import java.util.List;
import java.util.UUID;

public record GameStateResponse(
        UUID gameId,
        long dayNumber,
        int wordLength,
        int maxGuesses,
        List<GuessResult> guesses,
        GameStatus status,
        // Only populated once the game has ended (WON or LOST), so the answer
        // is never leaked to the client mid-game.
        String answer
) {
}
