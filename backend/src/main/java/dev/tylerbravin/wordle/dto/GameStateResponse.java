package dev.tylerbravin.wordle.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full state of a single game, returned by every {@code /api/game/**} endpoint.
 *
 * @param gameId           unique id for this game session
 * @param mode             {@link GameMode#DAILY}, {@link GameMode#ENDLESS}, or {@link GameMode#CUSTOM}
 * @param roundNumber      for DAILY, the calendar day index since the configured epoch;
 *                         for ENDLESS, this round's 1-based position within the current
 *                         shuffle-bag cycle (see {@link dev.tylerbravin.wordle.service.EndlessBagService});
 *                         unused (0) for CUSTOM
 * @param wordLength       length of the answer word - fixed at {@code wordle.word-length} for
 *                         DAILY/ENDLESS, but varies per puzzle (3-8) for CUSTOM
 * @param maxGuesses       number of guesses allowed before the game is lost
 * @param guesses          guesses submitted so far, in order, with per-letter feedback
 * @param status           current game status
 * @param answer           the answer word, populated only once {@code status} is WON or
 *                         LOST so it's never leaked to the client mid-game
 * @param nextDailyResetAt for DAILY games, the next UTC-midnight boundary - i.e. when
 *                         a new Daily word unlocks. {@code null} for ENDLESS/CUSTOM, which
 *                         have no equivalent global reset.
 */
public record GameStateResponse(
        UUID gameId,
        GameMode mode,
        long roundNumber,
        int wordLength,
        int maxGuesses,
        List<GuessResult> guesses,
        GameStatus status,
        String answer,
        Instant nextDailyResetAt
) {
}
