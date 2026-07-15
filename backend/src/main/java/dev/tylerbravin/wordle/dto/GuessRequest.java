package dev.tylerbravin.wordle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuessRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{5}$", message = "Guess must be exactly 5 letters")
        String guess
) {
}
