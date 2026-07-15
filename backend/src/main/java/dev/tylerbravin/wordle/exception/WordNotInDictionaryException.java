package dev.tylerbravin.wordle.exception;

public class WordNotInDictionaryException extends RuntimeException {
    public WordNotInDictionaryException(String word) {
        super("'" + word + "' is not a recognized word");
    }
}
