package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import dev.tylerbravin.wordle.dto.EndlessSessionResponse;
import dev.tylerbravin.wordle.dto.GameMode;
import dev.tylerbravin.wordle.dto.GameStateResponse;
import dev.tylerbravin.wordle.dto.GameStatus;
import dev.tylerbravin.wordle.dto.GuessResult;
import dev.tylerbravin.wordle.dto.LetterResult;
import dev.tylerbravin.wordle.exception.GameAlreadyFinishedException;
import dev.tylerbravin.wordle.exception.GameNotFoundException;
import dev.tylerbravin.wordle.exception.WordNotInDictionaryException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory orchestration for both game modes: resolving the answer word for a new
 * game, validating and scoring guesses, and tracking session state.
 * <p>
 * Session state lives in a {@link ConcurrentHashMap} keyed by {@code gameId}, which
 * is fine for a single-instance portfolio deployment; a production service handling
 * real traffic would back this with Redis (or similar) so sessions survive restarts
 * and the app can scale horizontally.
 */
@Service
public class GameService {

    private final WordService wordService;
    private final EndlessBagService endlessBagService;
    private final GuessEvaluator guessEvaluator;
    private final GameProperties properties;
    private final Clock clock;
    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();

    public GameService(
            WordService wordService,
            EndlessBagService endlessBagService,
            GuessEvaluator guessEvaluator,
            GameProperties properties,
            Clock clock
    ) {
        this.wordService = wordService;
        this.endlessBagService = endlessBagService;
        this.guessEvaluator = guessEvaluator;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Starts today's Daily game. Every player who calls this on the same calendar
     * day gets a fresh session for the same answer word.
     * <p>
     * "Today" is pinned to UTC rather than the host's default timezone, since that
     * default can silently differ between local dev, Docker, and wherever this
     * ends up deployed - and the day boundary needs to be a single, predictable
     * moment for the {@code nextDailyResetAt} countdown in {@link #toResponse} to
     * mean anything.
     *
     * @return state for the newly created game
     */
    public GameStateResponse startDailyGame() {
        LocalDate today = LocalDate.now(clock);
        String answer = wordService.wordForDay(today);
        long roundNumber = wordService.dayNumber(today);

        GameSession session = new GameSession(
                UUID.randomUUID(), GameMode.DAILY, roundNumber, answer, properties.maxGuesses());
        sessions.put(session.id(), session);

        return toResponse(session);
    }

    /**
     * Starts (or continues) an Endless session. If {@code rawPlayerId} names an
     * existing shuffle bag it is reused so the no-repeat guarantee holds across
     * rounds; otherwise a brand new bag is created.
     *
     * @param rawPlayerId a previously issued playerId, or {@code null}/blank for a new bag
     * @return the new round's game state plus bag bookkeeping the client should persist
     */
    public EndlessSessionResponse startEndlessGame(String rawPlayerId) {
        UUID playerId = resolveOrCreatePlayer(rawPlayerId);

        EndlessBagService.DealtWord dealt = endlessBagService.nextWord(playerId);

        GameSession session = new GameSession(
                UUID.randomUUID(), GameMode.ENDLESS, dealt.position(), dealt.word(), properties.maxGuesses());
        sessions.put(session.id(), session);

        return new EndlessSessionResponse(
                toResponse(session),
                playerId.toString(),
                endlessBagService.wordsRemaining(playerId),
                dealt.totalWords()
        );
    }

    /**
     * Fetches the current state of an existing game, e.g. after a page reload.
     * <p>
     * For DAILY games specifically, a session created on an earlier calendar day
     * is treated as {@link GameNotFoundException not found} rather than being
     * resumed as-is - otherwise a player whose browser still has yesterday's
     * (or any earlier) {@code gameId} cached would keep seeing that old day's
     * word forever, since nothing else would ever prompt a fresh session to be
     * created. The frontend already falls back to starting a new game whenever
     * this call fails for any reason, so this reuses that path rather than
     * needing special handling on the client.
     *
     * @param gameId id returned by a previous start/guess call
     * @return current state, with the answer populated if the game has ended
     * @throws GameNotFoundException if no session exists for this id, or if it's
     *         a DAILY session that no longer belongs to today
     */
    public GameStateResponse getGame(UUID gameId) {
        GameSession session = requireSession(gameId);
        if (session.mode() == GameMode.DAILY && session.roundNumber() != wordService.dayNumber(LocalDate.now(clock))) {
            throw new GameNotFoundException(gameId);
        }
        return toResponse(session);
    }

    /**
     * Scores a guess against the game's answer, records it, and advances game status.
     *
     * @param gameId id of an in-progress game
     * @param rawGuess the guessed word, any casing
     * @return updated state, including the answer if this guess won or lost the game
     * @throws GameNotFoundException if no session exists for this id
     * @throws GameAlreadyFinishedException if the game already ended
     * @throws WordNotInDictionaryException if the guess isn't a recognized word
     */
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

    private UUID resolveOrCreatePlayer(String rawPlayerId) {
        if (rawPlayerId != null && !rawPlayerId.isBlank()) {
            try {
                UUID parsed = UUID.fromString(rawPlayerId.trim());
                if (endlessBagService.hasPlayer(parsed)) {
                    return parsed;
                }
            } catch (IllegalArgumentException ignored) {
                // Not a valid UUID - fall through and issue a new one.
            }
        }
        return endlessBagService.createPlayer();
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
        Instant nextDailyResetAt = session.mode() == GameMode.DAILY ? nextUtcMidnight() : null;
        return new GameStateResponse(
                session.id(),
                session.mode(),
                session.roundNumber(),
                properties.wordLength(),
                session.maxGuesses(),
                session.guesses(),
                session.status(),
                revealedAnswer,
                nextDailyResetAt
        );
    }

    /** The next UTC-midnight boundary from right now - i.e. when the next Daily word unlocks. */
    private Instant nextUtcMidnight() {
        return LocalDate.now(clock).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
