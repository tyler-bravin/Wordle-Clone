package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import dev.tylerbravin.wordle.exception.PlayerNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndlessBagServiceTest {

    private final WordService wordService = new WordService(
            new GameProperties(6, 5, LocalDate.of(2024, 1, 1), 20240101L, "https://example.invalid")
    );
    private final EndlessBagService bagService = new EndlessBagService(wordService);

    @Test
    void dealsEveryWordExactlyOnceBeforeAnyRepeat() {
        UUID playerId = bagService.createPlayer();
        int total = bagService.totalWords();

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < total; i++) {
            String word = bagService.nextWord(playerId).word();
            boolean wasNew = seen.add(word);
            assertThat(wasNew)
                    .as("word '%s' repeated before the bag of %d words was exhausted", word, total)
                    .isTrue();
        }
        assertThat(seen).hasSize(total);
    }

    @Test
    void reshufflesAndKeepsDealingAfterExhaustion() {
        UUID playerId = bagService.createPlayer();
        int total = bagService.totalWords();

        for (int i = 0; i < total; i++) {
            bagService.nextWord(playerId);
        }
        assertThat(bagService.wordsRemaining(playerId)).isZero();

        // One more deal should transparently reshuffle a full new bag.
        EndlessBagService.DealtWord dealt = bagService.nextWord(playerId);
        assertThat(dealt.word()).isNotBlank();
        assertThat(bagService.wordsRemaining(playerId)).isEqualTo(total - 1);
    }

    @Test
    void positionIncreasesMonotonicallyWithinACycle() {
        UUID playerId = bagService.createPlayer();
        int first = bagService.nextWord(playerId).position();
        int second = bagService.nextWord(playerId).position();
        assertThat(second).isEqualTo(first + 1);
    }

    @Test
    void unknownPlayerIdThrows() {
        assertThatThrownBy(() -> bagService.nextWord(UUID.randomUUID()))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void separatePlayersGetIndependentBags() {
        UUID playerA = bagService.createPlayer();
        UUID playerB = bagService.createPlayer();

        String firstForA = bagService.nextWord(playerA).word();

        // Draining player B's whole bag shouldn't affect player A's remaining count.
        for (int i = 0; i < bagService.totalWords(); i++) {
            bagService.nextWord(playerB);
        }

        assertThat(bagService.wordsRemaining(playerA)).isEqualTo(bagService.totalWords() - 1);
        assertThat(firstForA).isNotBlank();
    }
}
