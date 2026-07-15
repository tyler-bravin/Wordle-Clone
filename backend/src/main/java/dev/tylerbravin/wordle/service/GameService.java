package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import dev.tylerbravin.wordle.dto.GameStateResponse;
import dev.tylerbravin.wordle.dto.GameStatus;
import dev.tylerbravin.wordle.dto.GuessResult;
import dev.tylerbravin.wordle.dto.LetterResult;
import dev.tylerbravin.wordle.exception.GameAlreadyFinishedException;
import dev.tylerbravin.wordle.exception.GameNotFoundException;
import dev.tylerbravin.wordle.exception.WordNotInDictionaryException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory game orchestration. State lives in a ConcurrentHashMap keyed by gameId,
 * which is fine for a single-instance portfolio deployment; a real production service
 * would back this with Redis so sessions survive restarts and scale horizontally.
 */
@Service
public class GameService {

    private final WordService wordService;
    private final GuessEvaluator guessEvaluator;
    private final GameProperties properties;
    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();

    public GameService(WordService wordService, GuessEvaluator guessEvaluator, GameProperties properties) {
        this.wordService = wordService;
        this.guessEvaluator = guessEvaluator;
        this.properties = properties;
    }

    public GameStateResponse startGame() {
        LocalDate today = LocalDate.now();
        UUID id = UUID.randomUUID();
        String answer = wordService.wordForDay(today);
        long dayNumber = wordService.dayNumber(today);

        GameSession session = new GameSession(id, today, dayNumber, answer, properties.maxGuesses());
        sessions.put(id, session);

        return toResponse(session);
    }

    public GameStateResponse getGame(UUID gameId) {
        return toResponse(requireSession(gameId));
    }

    public GameStateResponse submitGuess(UUID gameId, String rawGuess) {
        GameSession session = requireSession(gameId);

        if (session.isFinished()) {
            throw new GameAlreadyFinishedException();
        }

        String guess = rawGuess.trim().toLowerCase();
        if (!wordService.isValidGuess(guess)) {
            throw new WordNotInDictionaryException(guess);
        }

        var letterResults = guessEvaluator.evaluate(guess, session.answer());
        session.addGuess(new GuessResult(guess, letterResults));

        boolean won = letterResults.stream().allMatch(r -> r == LetterResult.CORRECT);
        if (won) {
            session.setStatus(GameStatus.WON);
        } else if (session.guesses().size() >= session.maxGuesses()) {
            session.setStatus(GameStatus.LOST);
        }

        return toResponse(session);
    }

    private GameSession requireSession(UUID gameId) {
        GameSession session = sessions.get(gameId);
        if (session == null) {
            throw new GameNotFoundException(gameId);
        }
        return session;
    }

    private GameStateResponse toResponse(GameSession session) {
        String revealedAnswer = session.isFinished() ? session.answer() : null;
        return new GameStateResponse(
                session.id(),
                session.dayNumber(),
                properties.wordLength(),
                session.maxGuesses(),
                session.guesses(),
                session.status(),
                revealedAnswer
        );
    }
}
