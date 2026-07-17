package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.exception.InvalidCustomWordException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validates and stores Custom puzzles: a player-chosen word plus guess count,
 * shared as a link anyone can start a fresh attempt against (see
 * {@link GameService#startCustomGame}).
 * <p>
 * Unlike Daily/Endless, a Custom word isn't checked against the curated
 * {@code allowed.txt}/{@code answers.txt} dictionaries - those are filtered to
 * one fixed length and specific to the original Wordle answer set. Instead,
 * "is this a real English word" is checked once, here, at creation time via
 * {@link DictionaryService#lookup} - the same lookup used for the post-game
 * definition flourish, just repurposed as an existence check. Guesses
 * submitted against a Custom game are never dictionary-checked at all - see
 * {@link GameService#submitGuess}.
 */
@Service
public class CustomPuzzleService {

    public static final int MIN_WORD_LENGTH = 3;
    public static final int MAX_WORD_LENGTH = 8;
    static final int MIN_GUESSES = 3;
    static final int MAX_GUESSES = 8;
    static final int MIN_EXPIRY_HOURS = 1;
    static final int MAX_EXPIRY_HOURS = 48;

    private static final Pattern ALPHABETIC = Pattern.compile("^[a-z]+$");

    private final DictionaryService dictionaryService;
    private final CustomPuzzleStore puzzleStore;
    private final Clock clock;
    private final Set<String> denylist;

    public CustomPuzzleService(DictionaryService dictionaryService, CustomPuzzleStore puzzleStore, Clock clock) {
        this.dictionaryService = dictionaryService;
        this.puzzleStore = puzzleStore;
        this.clock = clock;
        this.denylist = loadDenylist();
    }

    /**
     * Validates and stores a new Custom puzzle.
     *
     * @param rawWord        the word to guess, any casing/whitespace
     * @param maxGuesses     guesses allowed, must be within {@value MIN_GUESSES}-{@value MAX_GUESSES}
     * @param expiresInHours how long the link stays playable, {@value MIN_EXPIRY_HOURS}-{@value MAX_EXPIRY_HOURS}
     *                       hours from now - links aren't permanent by default, the creator picks how long
     * @return the id of the newly created puzzle, for building a shareable {@code /custom/{id}} link
     * @throws InvalidCustomWordException if the word, guess count, or expiry fails any validation step
     */
    public UUID createPuzzle(String rawWord, int maxGuesses, int expiresInHours) {
        String word = rawWord == null ? "" : rawWord.trim().toLowerCase();

        if (maxGuesses < MIN_GUESSES || maxGuesses > MAX_GUESSES) {
            throw new InvalidCustomWordException(
                    "Guess count must be between " + MIN_GUESSES + " and " + MAX_GUESSES);
        }
        if (expiresInHours < MIN_EXPIRY_HOURS || expiresInHours > MAX_EXPIRY_HOURS) {
            throw new InvalidCustomWordException(
                    "Link expiry must be between " + MIN_EXPIRY_HOURS + " and " + MAX_EXPIRY_HOURS + " hours");
        }
        if (word.length() < MIN_WORD_LENGTH || word.length() > MAX_WORD_LENGTH || !ALPHABETIC.matcher(word).matches()) {
            throw new InvalidCustomWordException(
                    "Word must be " + MIN_WORD_LENGTH + "-" + MAX_WORD_LENGTH + " letters, no spaces or punctuation");
        }
        if (denylist.contains(word)) {
            throw new InvalidCustomWordException("That word isn't allowed");
        }
        if (!dictionaryService.lookup(word).found()) {
            throw new InvalidCustomWordException("'" + word + "' isn't a recognized English word");
        }

        Instant now = clock.instant();
        CustomPuzzle puzzle = new CustomPuzzle(
                UUID.randomUUID(), word, maxGuesses, now, now.plus(Duration.ofHours(expiresInHours)));
        puzzleStore.save(puzzle);
        return puzzle.id();
    }

    private Set<String> loadDenylist() {
        Set<String> words = new HashSet<>();
        ClassPathResource resource = new ClassPathResource("words/denylist.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim().toLowerCase();
                if (!word.isEmpty()) {
                    words.add(word);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load denylist", e);
        }
        return words;
    }
}
