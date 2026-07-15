package dev.tylerbravin.wordle.dto;

import java.util.List;

/**
 * Response for {@code GET /api/dictionary/{word}}. Always returns 200 with
 * {@code found=false} rather than a 404 when no definition exists - a missing
 * dictionary entry is an expected, normal outcome for some valid Wordle
 * answers, not an error condition.
 *
 * @param word      the word looked up, lowercase
 * @param found     whether a definition was found at all
 * @param phonetic  IPA pronunciation, if the source provided one
 * @param meanings  up to a handful of distinct meanings the API returned, in order
 */
public record WordDefinitionResponse(
        String word,
        boolean found,
        String phonetic,
        List<Meaning> meanings
) {
    /**
     * @param partOfSpeech e.g. "noun", "verb"
     * @param definition   the definition text
     * @param example      an example sentence, if the source provided one
     */
    public record Meaning(String partOfSpeech, String definition, String example) {
    }

    public static WordDefinitionResponse notFound(String word) {
        return new WordDefinitionResponse(word, false, null, List.of());
    }
}
