package dev.tylerbravin.wordle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "wordle")
public record GameProperties(
        int maxGuesses,
        int wordLength,
        LocalDate epochDate,
        long shuffleSeed
) {
}
