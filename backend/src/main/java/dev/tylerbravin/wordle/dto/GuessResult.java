package dev.tylerbravin.wordle.dto;

import java.util.List;

/**
 * A single submitted guess together with its per-letter feedback.
 *
 * @param guess   the word that was guessed, lowercase
 * @param results per-letter feedback, same length and order as {@code guess}
 */
public record GuessResult(
        String guess,
        List<LetterResult> results
) {
}
