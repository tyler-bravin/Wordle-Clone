package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WordServiceTest {

    private final WordService service = new WordService(new GameProperties(
            6, 5, LocalDate.of(2024, 1, 1), 20240101L, "https://example.invalid", "https://fallback.invalid",
            Duration.ofDays(2), Duration.ofDays(30)));

    @Test
    void isValidGuessAcceptsAKnownFiveLetterWord() {
        assertThat(service.isValidGuess("crane")).isTrue();
    }

    @Test
    void isValidGuessRejectsGibberish() {
        assertThat(service.isValidGuess("zzqxx")).isFalse();
    }

    @Test
    void isValidGuessIsCaseInsensitive() {
        assertThat(service.isValidGuess("CRANE")).isTrue();
    }

    @Test
    void isValidCustomGuessAcceptsWordsOutsideTheFiveLetterCuratedList() {
        // "battery" (7) and "sky" (3) are real words the curated 5-letter-only
        // Daily/Endless dictionary can't contain by construction.
        assertThat(service.isValidCustomGuess("battery")).isTrue();
        assertThat(service.isValidCustomGuess("sky")).isTrue();
    }

    @Test
    void isValidCustomGuessRejectsGibberish() {
        assertThat(service.isValidCustomGuess("zzqxxz")).isFalse();
    }

    @Test
    void isValidCustomGuessIsCaseInsensitive() {
        assertThat(service.isValidCustomGuess("CRANE")).isTrue();
    }
}
