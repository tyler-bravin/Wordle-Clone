package dev.tylerbravin.wordle.dto;

import java.util.List;

public record GuessResult(
        String guess,
        List<LetterResult> results
) {
}
