package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import dev.tylerbravin.wordle.dto.EndlessSessionResponse;
import dev.tylerbravin.wordle.dto.GameMode;
import dev.tylerbravin.wordle.dto.GameStateResponse;
import dev.tylerbravin.wordle.exception.GameNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {

    private final GameProperties properties =
            new GameProperties(6, 5, LocalDate.of(2024, 1, 1), 20240101L, "https://example.invalid");
    private final WordService wordService = new WordService(properties);
    private final EndlessBagService endlessBagService = new EndlessBagService(wordService);

    // Anchored to a fixed instant rather than the real system clock, so these
    // tests are reproducible regardless of when they actually run - and, for
    // the day-boundary test below, so "a day passing" can be simulated by
    // just advancing this clock instead of needing to wait for real time to pass.
    private final MutableClock clock = new MutableClock(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
    private final GameService gameService =
            new GameService(wordService, endlessBagService, new GuessEvaluator(), properties, clock);

    @Test
    void dailyGameReportsAFutureUtcMidnightAsTheNextReset() {
        GameStateResponse response = gameService.startDailyGame();

        assertThat(response.mode()).isEqualTo(GameMode.DAILY);
        assertThat(response.nextDailyResetAt()).isNotNull();
        assertThat(response.nextDailyResetAt()).isAfter(clock.instant());
        // Should never be more than 24h out, since it's always "the next" midnight.
        assertThat(Duration.between(clock.instant(), response.nextDailyResetAt()))
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
        assertThat(fetched.nextDailyResetAt()).isAfter(clock.instant());
    }

    @Test
    void resumingADailyGameOnTheSameDayReturnsItNormally() {
        GameStateResponse started = gameService.startDailyGame();

        GameStateResponse resumed = gameService.getGame(started.gameId());

        assertThat(resumed.gameId()).isEqualTo(started.gameId());
        assertThat(resumed.roundNumber()).isEqualTo(started.roundNumber());
    }

    @Test
    void resumingADailyGameAfterMidnightIsTreatedAsNotFound() {
        // This is the actual bug report this test exists to catch: a browser
        // that still has yesterday's gameId cached should NOT keep resuming
        // yesterday's word forever just because nothing else prompted a fresh
        // session - see GameService.getGame's Javadoc for the reasoning.
        GameStateResponse startedYesterday = gameService.startDailyGame();

        clock.advanceTo(clock.instant().plus(Duration.ofDays(1)));

        assertThatThrownBy(() -> gameService.getGame(startedYesterday.gameId()))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void resumingAnEndlessGameAcrossMidnightStillWorks() {
        // Endless has no "belongs to a specific day" concept, unlike Daily -
        // an in-progress round should survive a day boundary just fine.
        EndlessSessionResponse started = gameService.startEndlessGame(null);

        clock.advanceTo(clock.instant().plus(Duration.ofDays(1)));

        GameStateResponse resumed = gameService.getGame(started.game().gameId());
        assertThat(resumed.gameId()).isEqualTo(started.game().gameId());
    }

    /** A Clock whose current instant can be advanced mid-test, to simulate day boundaries passing. */
    private static final class MutableClock extends Clock {
        private Instant now;
        private final ZoneId zone;

        MutableClock(Instant now, ZoneId zone) {
            this.now = now;
            this.zone = zone;
        }

        void advanceTo(Instant next) {
            this.now = next;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
