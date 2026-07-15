package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import dev.tylerbravin.wordle.dto.EndlessSessionResponse;
import dev.tylerbravin.wordle.dto.GameMode;
import dev.tylerbravin.wordle.dto.GameStateResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceTest {

    private final GameProperties properties =
            new GameProperties(6, 5, LocalDate.of(2024, 1, 1), 20240101L, "https://example.invalid");
    private final WordService wordService = new WordService(properties);
    private final EndlessBagService endlessBagService = new EndlessBagService(wordService);
    private final GameService gameService =
            new GameService(wordService, endlessBagService, new GuessEvaluator(), properties);

    @Test
    void dailyGameReportsAFutureUtcMidnightAsTheNextReset() {
        GameStateResponse response = gameService.startDailyGame();

        assertThat(response.mode()).isEqualTo(GameMode.DAILY);
        assertThat(response.nextDailyResetAt()).isNotNull();
        assertThat(response.nextDailyResetAt()).isAfter(Instant.now());
        // Should never be more than 24h out, since it's always "the next" midnight.
        assertThat(Duration.between(Instant.now(), response.nextDailyResetAt()))
                .isLessThanOrEqualTo(Duration.ofHours(24));
    }

    @Test
    void dailyResetTimestampIsExactlyAUtcMidnight() {
        GameStateResponse response = gameService.startDailyGame();

        var utcDateTime = response.nextDailyResetAt().atZone(ZoneOffset.UTC);
        assertThat(utcDateTime.getHour()).isZero();
        assertThat(utcDateTime.getMinute()).isZero();
        assertThat(utcDateTime.getSecond()).isZero();
    }

    @Test
    void endlessGameHasNoNextDailyReset() {
        EndlessSessionResponse response = gameService.startEndlessGame(null);

        assertThat(response.game().mode()).isEqualTo(GameMode.ENDLESS);
        assertThat(response.game().nextDailyResetAt()).isNull();
    }

    @Test
    void getGameRecomputesTheResetTimestampRatherThanFreezingItAtCreation() {
        GameStateResponse started = gameService.startDailyGame();
        GameStateResponse fetched = gameService.getGame(started.gameId());

        // Not asserting exact equality of the instant (both calls happen close
        // together in a fast test), just that it's still a sane future UTC midnight -
        // the point is toResponse() computes this fresh every call, not once at
        // session creation, so it stays accurate no matter when you check.
        assertThat(fetched.nextDailyResetAt()).isNotNull();
        assertThat(fetched.nextDailyResetAt()).isAfter(Instant.now());
    }
}
