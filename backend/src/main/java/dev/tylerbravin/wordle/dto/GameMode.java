package dev.tylerbravin.wordle.dto;

/**
 * Distinguishes the two ways a {@code GameSession} can be created.
 * <ul>
 *   <li>{@link #DAILY} - one shared word per calendar day, same for every player.</li>
 *   <li>{@link #ENDLESS} - an unlimited sequence of rounds drawn from a per-player
 *       shuffle bag that never repeats a word until the whole answer pool has been used.</li>
 * </ul>
 */
public enum GameMode {
    DAILY,
    ENDLESS
}
