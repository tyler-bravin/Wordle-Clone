package dev.tylerbravin.wordle.exception;

/** Thrown when Hard Mode is changed on a session after guessing has already started, or on a
 *  CUSTOM session (whose Hard Mode setting is the puzzle creator's choice, not the guesser's). */
public class HardModeChangeNotAllowedException extends RuntimeException {
    public HardModeChangeNotAllowedException(String message) {
        super(message);
    }
}
