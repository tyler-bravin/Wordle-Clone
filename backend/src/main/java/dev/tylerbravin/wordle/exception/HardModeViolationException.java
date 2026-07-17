package dev.tylerbravin.wordle.exception;

/** Thrown when a guess breaks a Hard Mode constraint - see {@code HardModeValidator}. */
public class HardModeViolationException extends RuntimeException {
    public HardModeViolationException(String message) {
        super(message);
    }
}
