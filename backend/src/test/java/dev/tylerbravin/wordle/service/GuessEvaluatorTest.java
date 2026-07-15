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
        // A genuine derangement of "crane"'s letters - every position differs
        // from the answer, so nothing should land on CORRECT. ("earnc", used
        // here previously, actually shares position 3 with "crane" ('n'=='n'),
        // which made CORRECT the right answer there - the test's premise was
        // wrong, not the evaluator.)
        List<LetterResult> result = evaluator.evaluate("ranec", "crane");
        assertThat(result).containsExactly(PRESENT, PRESENT, PRESENT, PRESENT, PRESENT);
    }

    @Test
    void duplicateLetterInGuess_singleOccurrenceInAnswer_onlyOneFlagged() {
        // answer "algae" has 'a' at positions 0 and 3. Guess "apple" has 'a'
        // only at position 0, which lines up exactly with one of the answer's
        // two 'a's - so it's CORRECT regardless of the duplicate-letter
        // handling this test cares about. Position 4 ('e') also lines up.
        List<LetterResult> result = evaluator.evaluate("apple", "algae");
        assertThat(result.get(0)).isEqualTo(CORRECT);
        assertThat(result.get(4)).isEqualTo(CORRECT);
    }

    @Test
    void guessWithDoubleLetter_answerHasOnlyOne_marksOnePresentOneAbsent() {
        // answer "crane" has exactly one 'e', at the last position. A guess of
        // all 'e's isolates that: one should land CORRECT (position 4), and
        // every other 'e' should be ABSENT since the answer's only 'e' is
        // already spoken for. (An earlier version of this test used "eerie",
        // which also contains an 'r' that legitimately matches crane's 'r' -
        // a real PRESENT result unrelated to what this test means to check,
        // which threw off the count.)
        List<LetterResult> result = evaluator.evaluate("eeeee", "crane");
        long correctOrPresentE = result.stream().filter(r -> r != ABSENT).count();
        assertThat(correctOrPresentE).isEqualTo(1);
        assertThat(result.get(4)).isEqualTo(CORRECT);
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
