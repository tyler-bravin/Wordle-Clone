package dev.tylerbravin.wordle.controller;

import dev.tylerbravin.wordle.dto.WordDefinitionResponse;
import dev.tylerbravin.wordle.exception.WordNotInDictionaryException;
import dev.tylerbravin.wordle.service.CustomPuzzleService;
import dev.tylerbravin.wordle.service.DictionaryService;
import dev.tylerbravin.wordle.service.WordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * Serves the post-game "what does this word mean" lookup. Kept as its own
 * endpoint rather than folded into {@code GameStateResponse} since it's an
 * optional flourish the frontend fetches once a game ends, not part of core
 * game state.
 */
@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {

    // Custom puzzle answers aren't in WordService's curated (fixed-length) list -
    // they were already validated as real words at creation time (see
    // CustomPuzzleService), so any word in that same length range is accepted
    // here too, rather than only ever-5-letter DAILY/ENDLESS answers.
    private static final Pattern CUSTOM_LENGTH_RANGE = Pattern.compile(
            "^[a-z]{" + CustomPuzzleService.MIN_WORD_LENGTH + "," + CustomPuzzleService.MAX_WORD_LENGTH + "}$");

    private final DictionaryService dictionaryService;
    private final WordService wordService;

    public DictionaryController(DictionaryService dictionaryService, WordService wordService) {
        this.dictionaryService = dictionaryService;
        this.wordService = wordService;
    }

    /**
     * Looks up a definition for a word from this game's own dictionary.
     * Deliberately restricted to words this game could actually have as an
     * answer (rather than proxying arbitrary strings to the external API) so
     * this endpoint can't be used as a general-purpose dictionary proxy
     * unrelated to the game.
     *
     * @param word the word to define
     * @return 200 with {@code found=false} if no definition exists - see
     *         {@link DictionaryService} for why that's not a 404
     * @throws WordNotInDictionaryException if the word isn't one this game recognizes at all
     */
    @GetMapping("/{word}")
    public ResponseEntity<WordDefinitionResponse> getDefinition(@PathVariable String word) {
        String normalized = word.trim().toLowerCase();
        boolean recognized = wordService.isValidGuess(normalized) || CUSTOM_LENGTH_RANGE.matcher(normalized).matches();
        if (!recognized) {
            throw new WordNotInDictionaryException(normalized);
        }
        return ResponseEntity.ok(dictionaryService.lookup(normalized));
    }
}
