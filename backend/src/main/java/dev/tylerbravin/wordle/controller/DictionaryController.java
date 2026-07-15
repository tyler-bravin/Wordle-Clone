package dev.tylerbravin.wordle.controller;

import dev.tylerbravin.wordle.dto.WordDefinitionResponse;
import dev.tylerbravin.wordle.exception.WordNotInDictionaryException;
import dev.tylerbravin.wordle.service.DictionaryService;
import dev.tylerbravin.wordle.service.WordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the post-game "what does this word mean" lookup. Kept as its own
 * endpoint rather than folded into {@code GameStateResponse} since it's an
 * optional flourish the frontend fetches once a game ends, not part of core
 * game state.
 */
@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {

    private final DictionaryService dictionaryService;
    private final WordService wordService;

    public DictionaryController(DictionaryService dictionaryService, WordService wordService) {
        this.dictionaryService = dictionaryService;
        this.wordService = wordService;
    }

    /**
     * Looks up a definition for a word from this game's own dictionary.
     * Deliberately restricted to words {@link WordService} already recognizes
     * (rather than proxying arbitrary strings to the external API) so this
     * endpoint can't be used as a general-purpose dictionary proxy unrelated
     * to the game.
     *
     * @param word the word to define
     * @return 200 with {@code found=false} if no definition exists - see
     *         {@link DictionaryService} for why that's not a 404
     * @throws WordNotInDictionaryException if the word isn't one this game recognizes at all
     */
    @GetMapping("/{word}")
    public ResponseEntity<WordDefinitionResponse> getDefinition(@PathVariable String word) {
        String normalized = word.trim().toLowerCase();
        if (!wordService.isValidGuess(normalized)) {
            throw new WordNotInDictionaryException(normalized);
        }
        return ResponseEntity.ok(dictionaryService.lookup(normalized));
    }
}
