package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import dev.tylerbravin.wordle.dto.EndlessSessionResponse;
import dev.tylerbravin.wordle.dto.GameMode;
import dev.tylerbravin.wordle.dto.GameStateResponse;
import dev.tylerbravin.wordle.dto.GameStatus;
import dev.tylerbravin.wordle.dto.GuessResult;
import dev.tylerbravin.wordle.dto.LetterResult;
import dev.tylerbravin.wordle.exception.CustomPuzzleNotFoundException;
import dev.tylerbravin.wordle.exception.GameAlreadyFinishedException;
import dev.tylerbravin.wordle.exception.GameNotFoundException;
import dev.tylerbravin.wordle.exception.WordNotInDictionaryException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Orchestration for both game modes: resolving the answer word for a new game,
 * validating and scoring guesses, and tracking session state.
 * <p>
 * Session state is persisted via {@link GameSessionStore} (Redis-backed in
 * production), keyed by {@code gameId}, so sessions survive a backend restart.
 */
@Service
public class GameService {

    private final WordService wordService;
    private final EndlessBagService endlessBagService;
    private final GuessEvaluator guessEvaluator;
    private final GameProperties properties;
    private final Clock clock;
    private final GameSessionStore sessionStore;
    private final CustomPuzzleStore customPuzzleStore;

    public GameService(
            WordService wordService,
            EndlessBagService endlessBagService,
            GuessEvaluator guessEvaluator,
            GameProperties properties,
            Clock clock,
            GameSessionStore sessionStore,
            CustomPuzzleStore customPuzzleStore
    ) {
        this.wordService = wordService;
        this.endlessBagService = endlessBagService;
        this.guessEvaluator = guessEvaluator;
        this.properties = properties;
        this.clock = clock;
        this.sessionStore = sessionStore;
        this.customPuzzleStore = customPuzzleStore;
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

        GameSession session = GameSession.start(
                UUID.randomUUID(), GameMode.DAILY, roundNumber, answer, properties.maxGuesses());
        sessionStore.save(session);

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

        GameSession session = GameSession.start(
                UUID.randomUUID(), GameMode.ENDLESS, dealt.position(), dealt.word(), properties.maxGuesses());
        sessionStore.save(session);

        return new EndlessSessionResponse(
                toResponse(session),
                playerId.toString(),
                endlessBagService.wordsRemaining(playerId),
                dealt.totalWords()
        );
    }

    /**
     * Starts a fresh attempt at an existing Custom puzzle. Unlike Daily/Endless,
     * many independent sessions can exist for the same puzzle - anyone with the
     * link gets their own attempt, none of which affect each other.
     *
     * @param puzzleId id of a puzzle previously created via {@link CustomPuzzleService#createPuzzle}
     * @return state for the newly created session
     * @throws CustomPuzzleNotFoundException if no puzzle exists for this id (e.g. it expired)
     */
    public GameStateResponse startCustomGame(UUID puzzleId) {
        CustomPuzzle puzzle = customPuzzleStore.find(puzzleId)
                .orElseThrow(() -> new CustomPuzzleNotFoundException(puzzleId));

        GameSession session = GameSession.start(
                UUID.randomUUID(), GameMode.CUSTOM, 0, puzzle.word(), puzzle.maxGuesses());
        sessionStore.save(session);

        return toResponse(session);
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
     * @throws WordNotInDictionaryException if the guess isn't a recognized word (DAILY/ENDLESS only -
     *         CUSTOM guesses aren't dictionary-checked, see {@link CustomPuzzleService}'s Javadoc)
     */
    public GameStateResponse submitGuess(UUID gameId, String rawGuess) {
        GameSession session = requireSession(gameId);

        if (session.isFinished()) {
            throw new GameAlreadyFinishedException();
        }

        String guess = rawGuess.trim().toLowerCase();
        if (session.mode() != GameMode.CUSTOM && !wordService.isValidGuess(guess)) {
            throw new WordNotInDictionaryException(guess);
        }

        var letterResults = guessEvaluator.evaluate(guess, session.answer());
        GuessResult result = new GuessResult(guess, letterResults);

        boolean won = letterResults.stream().allMatch(r -> r == LetterResult.CORRECT);
        GameStatus newStatus;
        if (won) {
            newStatus = GameStatus.WON;
        } else if (session.guesses().size() + 1 >= session.maxGuesses()) {
            newStatus = GameStatus.LOST;
        } else {
            newStatus = GameStatus.IN_PROGRESS;
        }

        GameSession updated = session.withGuess(result, newStatus);
        sessionStore.save(updated);

        return toResponse(updated);
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
        return sessionStore.find(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }

    private GameStateResponse toResponse(GameSession session) {
        String revealedAnswer = session.isFinished() ? session.answer() : null;
        Instant nextDailyResetAt = session.mode() == GameMode.DAILY ? nextUtcMidnight() : null;
        return new GameStateResponse(
                session.id(),
                session.mode(),
                session.roundNumber(),
                session.answer().length(),
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
