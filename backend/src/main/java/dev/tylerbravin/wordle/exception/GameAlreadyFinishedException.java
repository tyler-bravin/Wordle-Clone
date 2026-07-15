package dev.tylerbravin.wordle.exception;

public class GameAlreadyFinishedException extends RuntimeException {
    public GameAlreadyFinishedException() {
        super("This game has already ended - start a new game to keep playing");
    }
}
