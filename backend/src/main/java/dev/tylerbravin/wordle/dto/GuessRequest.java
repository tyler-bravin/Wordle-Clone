package dev.tylerbravin.wordle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code POST /api/game/{gameId}/guess}.
 * <p>
 * Length is only bounded to the 3-8 range Custom puzzles allow here - whether
 * it actually matches this session's answer is checked in
 * {@code GuessEvaluator}, since that depends on which game this guess is for.
 *
 * @param guess the guessed word, 3-8 letters, any casing
 */
public record GuessRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{3,8}$", message = "Guess must be 3-8 letters")
        String guess
) {
}
