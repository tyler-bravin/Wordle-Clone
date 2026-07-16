package dev.tylerbravin.wordle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Binds the {@code wordle.*} properties from {@code application.yml}.
 *
 * @param maxGuesses          how many guesses a player gets before a game is lost
 * @param wordLength          length of answer/guess words (the dictionaries assume 5)
 * @param epochDate           fixed reference date the Daily word cycle counts from;
 *                            changing this after launch reshuffles which word falls on which day
 * @param shuffleSeed         fixed seed used to shuffle the Daily answer order at startup
 * @param dictionaryApiBaseUrl base URL of the external dictionary API used for the
 *                            post-game "what does this word mean" lookup
 * @param sessionTtl          how long an idle game session lives in Redis before expiring
 * @param bagTtl              how long an idle Endless shuffle-bag lives in Redis before expiring
 */
@ConfigurationProperties(prefix = "wordle")
public record GameProperties(
        int maxGuesses,
        int wordLength,
        LocalDate epochDate,
        long shuffleSeed,
        String dictionaryApiBaseUrl,
        Duration sessionTtl,
        Duration bagTtl
) {
}
