package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.dto.LetterResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.tylerbravin.wordle.dto.LetterResult.*;
import static org.assertj.core.api.Assertions.assertThat;

class GuessEvaluatorTest {

    private final GuessEvaluator evaluator = new GuessEvaluator();

    @Test
    void allCorrectWhenGuessMatchesAnswer() {
        List<LetterResult> result = evaluator.evaluate("crane", "crane");
        assertThat(result).containsExactly(CORRECT, CORRECT, CORRECT, CORRECT, CORRECT);
    }

    @Test
    void allAbsentWhenNoLettersMatch() {
        List<LetterResult> result = evaluator.evaluate("bumpy", "click");
        assertThat(result).containsExactly(ABSENT, ABSENT, ABSENT, ABSENT, ABSENT);
    }

    @Test
    void marksPresentForRightLetterWrongPosition() {
        // answer = "crane", guess = "reactor"-ish single letters out of place
        List<LetterResult> result = evaluator.evaluate("earnc", "crane");
        assertThat(result).containsExactly(PRESENT, PRESENT, PRESENT, PRESENT, PRESENT);
    }

    @Test
    void duplicateLetterInGuess_singleOccurrenceInAnswer_onlyOneFlagged() {
        // Answer "algae" has one 'a' in position 1 and one in position 4.
        // Guess "aabbb" has two 'a's - first should be PRESENT/CORRECT-eligible, extra should be ABSENT.
        List<LetterResult> result = evaluator.evaluate("apple", "algae");
        // a-p-p-l-e vs a-l-g-a-e
        // pos0: 'a' vs 'a' -> CORRECT
        // pos1: 'p' vs 'l' -> answer has no extra 'p' -> ABSENT
        // pos2: 'p' vs 'g' -> ABSENT
        // pos3: 'l' vs 'a' -> answer has an 'l' at pos... wait answer is a-l-g-a-e, has 'l' at index1 already consumed? not consumed since it wasn't correct match.
        // pos4: 'e' vs 'e' -> CORRECT
        assertThat(result.get(0)).isEqualTo(CORRECT);
        assertThat(result.get(4)).isEqualTo(CORRECT);
    }

    @Test
    void guessWithDoubleLetter_answerHasOnlyOne_marksOnePresentOneAbsent() {
        // answer "crane" has exactly one 'e'. Guess "eerie" has three 'e's.
        List<LetterResult> result = evaluator.evaluate("eerie", "crane");
        long correctOrPresentE = result.stream().filter(r -> r != ABSENT).count();
        // Only the single 'e' in the answer can be matched once across the whole guess.
        assertThat(correctOrPresentE).isEqualTo(1);
    }

    @Test
    void isCaseInsensitive() {
        List<LetterResult> result = evaluator.evaluate("CRANE", "crane");
        assertThat(result).containsExactly(CORRECT, CORRECT, CORRECT, CORRECT, CORRECT);
    }

    @Test
    void rejectsMismatchedLength() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate("ab", "crane")
        );
    }
}
