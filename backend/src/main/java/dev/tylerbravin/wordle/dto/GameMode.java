package dev.tylerbravin.wordle.dto;

/**
 * Distinguishes the ways a {@code GameSession} can be created.
 * <ul>
 *   <li>{@link #DAILY} - one shared word per calendar day, same for every player.</li>
 *   <li>{@link #ENDLESS} - an unlimited sequence of rounds drawn from a per-player
 *       shuffle bag that never repeats a word until the whole answer pool has been used.</li>
 *   <li>{@link #CUSTOM} - a player-chosen word (via {@code CustomPuzzle}) shared as a
 *       link; guesses aren't checked against the curated dictionary the way Daily/
 *       Endless guesses are - see {@code GameService#submitGuess}.</li>
 * </ul>
 */
public enum GameMode {
    DAILY,
    ENDLESS,
    CUSTOM
}
