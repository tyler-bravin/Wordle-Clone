package dev.tylerbravin.wordle.exception;

/** Thrown when a word/guess-count submitted to create a Custom puzzle fails validation. */
public class InvalidCustomWordException extends RuntimeException {
    public InvalidCustomWordException(String reason) {
        super(reason);
    }
}
