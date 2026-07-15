package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Loads the answer list (curated solution words) and the full valid-guess dictionary
 * from the classpath at startup, and resolves which word applies for a given daily
 * puzzle or Endless round.
 * <p>
 * The bundled answer list ships in alphabetical order, so it is shuffled once at
 * startup with a fixed seed to produce a sequence that is stable across restarts
 * but not trivially guessable - the same approach the original Wordle used for its
 * day-by-day answer order.
 */
@Service
public class WordService {

    private final List<String> dailyAnswers;
    private final List<String> answerPool;
    private final Set<String> validGuesses;
    private final GameProperties properties;

    public WordService(GameProperties properties) {
        this.properties = properties;
        this.validGuesses = loadWordSet("words/allowed.txt");

        this.answerPool = List.copyOf(loadWordSet("words/answers.txt"));

        List<String> shuffledForDaily = new ArrayList<>(answerPool);
        Collections.shuffle(shuffledForDaily, new Random(properties.shuffleSeed()));
        this.dailyAnswers = List.copyOf(shuffledForDaily);

        if (!validGuesses.containsAll(answerPool)) {
            throw new IllegalStateException("Answer list contains words missing from the guess dictionary");
        }
    }

    /**
     * Returns the 0-indexed number of whole days between the configured epoch date
     * and the given date. Used to pick a stable index into {@link #dailyAnswers} so
     * every player sees the same word on the same calendar day.
     *
     * @param date the date to measure, typically {@link LocalDate#now()}
     * @return days elapsed since {@code wordle.epoch-date}; can be negative for dates before it
     */
    public long dayNumber(LocalDate date) {
        return ChronoUnit.DAYS.between(properties.epochDate(), date);
    }

    /**
     * Resolves the daily puzzle answer for the given date by indexing into the
     * fixed-seed-shuffled answer list, wrapping around once the list is exhausted.
     *
     * @param date the calendar date to resolve a word for
     * @return the 5-letter answer word, lowercase
     */
    public String wordForDay(LocalDate date) {
        long day = dayNumber(date);
        int index = (int) Math.floorMod(day, dailyAnswers.size());
        return dailyAnswers.get(index);
    }

    /**
     * Checks whether a word is accepted as a guess, i.e. present in the full
     * (much larger) valid-guess dictionary rather than only the curated answer list.
     *
     * @param word candidate guess, any casing
     * @return true if the word is a recognized 5-letter English word
     */
    public boolean isValidGuess(String word) {
        return validGuesses.contains(word.toLowerCase());
    }

    /**
     * The full pool of curated answer words in undefined (load) order. Intended for
     * callers such as {@link EndlessBagService} that need to build their own
     * independently-shuffled sequences, separate from the fixed daily order.
     *
     * @return an immutable list of every possible answer word
     */
    public List<String> answerPool() {
        return answerPool;
    }

    /**
     * Reads a newline-delimited word list from the classpath, lowercasing and
     * filtering to the configured word length.
     *
     * @param classpathLocation path under {@code src/main/resources}
     * @return the set of words found, never empty
     */
    private Set<String> loadWordSet(String classpathLocation) {
        Set<String> words = new HashSet<>();
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim().toLowerCase();
                if (word.length() == properties.wordLength()) {
                    words.add(word);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load word list: " + classpathLocation, e);
        }
        if (words.isEmpty()) {
            throw new IllegalStateException("Word list was empty: " + classpathLocation);
        }
        return words;
    }
}
