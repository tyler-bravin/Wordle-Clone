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
 * at startup, and resolves which word is "today's" answer.
 *
 * The answer list ships in alphabetical order, so it's shuffled once with a fixed
 * seed at startup to produce a stable but non-obvious daily sequence - the same
 * approach the original Wordle used.
 */
@Service
public class WordService {

    private final List<String> dailyAnswers;
    private final Set<String> validGuesses;
    private final GameProperties properties;

    public WordService(GameProperties properties) {
        this.properties = properties;
        this.validGuesses = loadWordSet("words/allowed.txt");

        List<String> answers = new ArrayList<>(loadWordSet("words/answers.txt"));
        Collections.shuffle(answers, new Random(properties.shuffleSeed()));
        this.dailyAnswers = List.copyOf(answers);

        // Every possible answer must also be an accepted guess.
        if (!validGuesses.containsAll(dailyAnswers)) {
            throw new IllegalStateException("Answer list contains words missing from the guess dictionary");
        }
    }

    /** 0-indexed day number since the configured epoch date, used to pick today's word. */
    public long dayNumber(LocalDate date) {
        return ChronoUnit.DAYS.between(properties.epochDate(), date);
    }

    /** Resolves the answer word for the given date, cycling through the shuffled list. */
    public String wordForDay(LocalDate date) {
        long day = dayNumber(date);
        int index = (int) Math.floorMod(day, dailyAnswers.size());
        return dailyAnswers.get(index);
    }

    public boolean isValidGuess(String word) {
        return validGuesses.contains(word.toLowerCase());
    }

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
