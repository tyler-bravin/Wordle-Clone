package dev.tylerbravin.wordle.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/custom}. Word length/character rules, the
 * guess-count range, and the expiry range are all enforced in
 * {@code CustomPuzzleService}, not here via annotations, so a rejection
 * carries a specific, user-facing reason rather than a generic validation message.
 *
 * @param word           the word to guess, any casing/whitespace
 * @param maxGuesses     guesses allowed for this puzzle
 * @param expiresInHours how long the link stays playable, from now
 */
public record CreateCustomPuzzleRequest(
        @NotBlank String word,
        int maxGuesses,
        int expiresInHours
) {
}
