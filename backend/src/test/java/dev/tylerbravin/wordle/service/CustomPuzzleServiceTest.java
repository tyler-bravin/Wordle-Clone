package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.dto.WordDefinitionResponse;
import dev.tylerbravin.wordle.exception.InvalidCustomWordException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DictionaryService} is mocked throughout - these tests exercise
 * {@link CustomPuzzleService}'s own validation rules, not the real dictionary
 * lookup (already covered by {@link DictionaryServiceTest}).
 */
class CustomPuzzleServiceTest {

    private final DictionaryService dictionaryService = mock(DictionaryService.class);
    private final InMemoryCustomPuzzleStore puzzleStore = new InMemoryCustomPuzzleStore();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
    private final CustomPuzzleService service = new CustomPuzzleService(dictionaryService, puzzleStore, clock);

    @Test
    void createsAndStoresAValidPuzzle() {
        wordExists("crane");

        UUID puzzleId = service.createPuzzle("crane", 5, 24);

        CustomPuzzle stored = puzzleStore.find(puzzleId).orElseThrow();
        assertThat(stored.word()).isEqualTo("crane");
        assertThat(stored.maxGuesses()).isEqualTo(5);
        assertThat(stored.createdAt()).isEqualTo(clock.instant());
        assertThat(stored.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofHours(24)));
    }

    @Test
    void normalizesCasingAndWhitespaceBeforeStoring() {
        wordExists("crane");

        UUID puzzleId = service.createPuzzle("  CRANE  ", 5, 24);

        assertThat(puzzleStore.find(puzzleId).orElseThrow().word()).isEqualTo("crane");
    }

    @Test
    void rejectsWordShorterThanMinimumLength() {
        assertThatThrownBy(() -> service.createPuzzle("ab", 5, 24))
                .isInstanceOf(InvalidCustomWordException.class);
    }

    @Test
    void rejectsWordLongerThanMaximumLength() {
        assertThatThrownBy(() -> service.createPuzzle("abcdefghi", 5, 24))
                .isInstanceOf(InvalidCustomWordException.class);
    }

    @Test
    void rejectsNonAlphabeticWord() {
        assertThatThrownBy(() -> service.createPuzzle("cra9e", 5, 24))
                .isInstanceOf(InvalidCustomWordException.class);
    }

    @Test
    void rejectsGuessCountBelowMinimum() {
        assertThatThrownBy(() -> service.createPuzzle("crane", 2, 24))
                .isInstanceOf(InvalidCustomWordException.class);
    }

    @Test
    void rejectsGuessCountAboveMaximum() {
        assertThatThrownBy(() -> service.createPuzzle("crane", 9, 24))
                .isInstanceOf(InvalidCustomWordException.class);
    }

    @Test
    void rejectsExpiryBelowMinimum() {
        assertThatThrownBy(() -> service.createPuzzle("crane", 5, 0))
                .isInstanceOf(InvalidCustomWordException.class);
    }

    @Test
    void rejectsExpiryAboveMaximum() {
        assertThatThrownBy(() -> service.createPuzzle("crane", 5, 49))
                .isInstanceOf(InvalidCustomWordException.class);
    }

    @Test
    void rejectsADenylistedWordRegardlessOfCasing() {
        assertThatThrownBy(() -> service.createPuzzle("KILL", 5, 24))
                .isInstanceOf(InvalidCustomWordException.class);
    }

    @Test
    void rejectsAWordTheDictionaryDoesNotRecognize() {
        when(dictionaryService.lookup(anyString())).thenReturn(WordDefinitionResponse.notFound("zzqxx"));

        assertThatThrownBy(() -> service.createPuzzle("zzqxx", 5, 24))
                .isInstanceOf(InvalidCustomWordException.class);
    }

    private void wordExists(String word) {
        when(dictionaryService.lookup(word)).thenReturn(new WordDefinitionResponse(word, true, null, List.of(
                new WordDefinitionResponse.Meaning("noun", "a definition", null)
        )));
    }
}
