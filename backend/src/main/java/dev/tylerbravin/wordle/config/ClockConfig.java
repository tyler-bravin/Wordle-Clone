package dev.tylerbravin.wordle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * A single injectable {@link Clock}, pinned to UTC, used everywhere the app
 * needs "now" instead of calling {@code Clock.systemUTC()} or
 * {@code LocalDate.now(ZoneOffset.UTC)} directly. Doing it this way - rather
 * than reaching for the system clock inline - is what makes day-boundary
 * behavior (like whether a Daily session belongs to today) actually
 * testable: tests can inject a fixed or mutable clock instead of being at
 * the mercy of whatever moment they happen to run at.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
