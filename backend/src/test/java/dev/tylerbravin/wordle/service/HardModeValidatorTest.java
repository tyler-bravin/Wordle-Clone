package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.dto.GuessResult;
import dev.tylerbravin.wordle.dto.LetterResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HardModeValidatorTest {

    private final GuessEvaluator guessEvaluator = new GuessEvaluator();
    private final HardModeValidator validator = new HardModeValidator();

    @Test
    void firstGuessAlwaysPasses() {
        assertThat(validator.validate("zzzzz", List.of())).isEmpty();
    }

    @Test
    void rejectsAGuessThatMovesALockedCorrectLetter() {
        // "board" vs "crane" locks position 3 (1-indexed) = 'a'.
        GuessResult previous = scored("board", "crane");

        Optional<String> violation = validator.validate("moist", List.of(previous));

        assertThat(violation).contains("3rd letter must be A");
    }

    @Test
    void rejectsAGuessMissingARequiredPresentLetter() {
        // "board" vs "crane" also reveals a PRESENT 'r'.
        GuessResult previous = scored("board", "crane");

        Optional<String> violation = validator.validate("slate", List.of(previous));

        assertThat(violation).contains("Guess must contain R");
    }

    @Test
    void acceptsAGuessThatSatisfiesAllConstraints() {
        GuessResult previous = scored("board", "crane");

        assertThat(validator.validate("crane", List.of(previous))).isEmpty();
    }

    @Test
    void requiresEnoughOccurrencesOfADuplicatedLetter() {
        // "grass" vs "sassy" reveals one CORRECT 's' and one PRESENT 's' - two
        // occurrences required overall, not just "contains an s".
        GuessResult previous = scored("grass", "sassy");

        // "raise" satisfies the locked position and includes an 'a', but only
        // has one 's' where two are required.
        Optional<String> violation = validator.validate("raise", List.of(previous));

        assertThat(violation).contains("Guess must contain S");
    }

    @Test
    void acceptsAGuessWithEnoughOccurrencesOfADuplicatedLetter() {
        GuessResult previous = scored("grass", "sassy");

        // "gassy" keeps the locked position and has two s's.
        assertThat(validator.validate("gassy", List.of(previous))).isEmpty();
    }

    @Test
    void takesTheMaxRequiredCountAcrossMultiplePreviousGuesses() {
        // Built directly rather than via GuessEvaluator so each guess reveals an
        // exact, isolated 's' count with no CORRECT positions to complicate things.
        GuessResult revealsOneS = new GuessResult("sabcd", List.of(
                LetterResult.PRESENT, LetterResult.ABSENT, LetterResult.ABSENT, LetterResult.ABSENT, LetterResult.ABSENT));
        GuessResult revealsTwoS = new GuessResult("ssabc", List.of(
                LetterResult.PRESENT, LetterResult.PRESENT, LetterResult.ABSENT, LetterResult.ABSENT, LetterResult.ABSENT));

        // Only one 's' - not enough once the second guess revealed there are two.
        Optional<String> violation = validator.validate("sabcd", List.of(revealsOneS, revealsTwoS));

        assertThat(violation).contains("Guess must contain S");
    }

    @Test
    void aGuessMeetingTheMaxRequiredCountPasses() {
        GuessResult revealsOneS = new GuessResult("sabcd", List.of(
                LetterResult.PRESENT, LetterResult.ABSENT, LetterResult.ABSENT, LetterResult.ABSENT, LetterResult.ABSENT));
        GuessResult revealsTwoS = new GuessResult("ssabc", List.of(
                LetterResult.PRESENT, LetterResult.PRESENT, LetterResult.ABSENT, LetterResult.ABSENT, LetterResult.ABSENT));

        assertThat(validator.validate("ssdef", List.of(revealsOneS, revealsTwoS))).isEmpty();
    }

    private GuessResult scored(String guess, String answer) {
        return new GuessResult(guess, guessEvaluator.evaluate(guess, answer));
    }
}
