package dev.tylerbravin.wordle.dto;

/**
 * Per-letter feedback for a submitted guess, matching classic Wordle semantics.
 */
public enum LetterResult {
    /** Letter is in the word and in the correct position. */
    CORRECT,
    /** Letter is in the word but in a different position. */
    PRESENT,
    /** Letter does not appear in the word (or all occurrences already accounted for). */
    ABSENT
}
