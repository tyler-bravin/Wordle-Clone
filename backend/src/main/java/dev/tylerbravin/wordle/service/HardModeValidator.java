package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.dto.GuessResult;
import dev.tylerbravin.wordle.dto.LetterResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enforces classic Wordle "Hard Mode" rules against a game's guess history:
 * once a letter is revealed {@link LetterResult#CORRECT} at a position, every
 * later guess must keep it there; once revealed {@link LetterResult#PRESENT}
 * (or {@code CORRECT} elsewhere), every later guess must include at least
 * that many occurrences of it somewhere.
 * <p>
 * Only used when {@code GameSession.hardMode()} is set - see
 * {@link GameService#submitGuess}, which runs this *before* scoring a guess,
 * so a violation is rejected without being recorded or consuming an attempt.
 */
@Component
public class HardModeValidator {

    /**
     * @param guess           the candidate guess, lowercase
     * @param previousGuesses guesses already submitted this game, in order
     * @return a user-facing violation message (e.g. "5th letter must be S",
     *         "Guess must contain E") if {@code guess} breaks a constraint
     *         established by {@code previousGuesses}, or empty if it's compliant.
     *         Always empty if {@code previousGuesses} is empty - hard mode has
     *         nothing to enforce against a game's first guess.
     */
    public Optional<String> validate(String guess, List<GuessResult> previousGuesses) {
        if (previousGuesses.isEmpty()) {
            return Optional.empty();
        }

        int length = previousGuesses.get(0).guess().length();
        Character[] lockedPositions = new Character[length];
        // Insertion order kept so violation messages are stable/predictable
        // rather than depending on hash order.
        Map<Character, Integer> requiredCounts = new LinkedHashMap<>();

        for (GuessResult previous : previousGuesses) {
            Map<Character, Integer> revealedThisGuess = new LinkedHashMap<>();
            List<LetterResult> results = previous.results();
            for (int i = 0; i < results.size(); i++) {
                char c = previous.guess().charAt(i);
                LetterResult result = results.get(i);
                if (result == LetterResult.CORRECT) {
                    lockedPositions[i] = c;
                    revealedThisGuess.merge(c, 1, Integer::sum);
                } else if (result == LetterResult.PRESENT) {
                    revealedThisGuess.merge(c, 1, Integer::sum);
                }
            }
            revealedThisGuess.forEach((c, count) -> requiredCounts.merge(c, count, Math::max));
        }

        for (int i = 0; i < lockedPositions.length; i++) {
            Character required = lockedPositions[i];
            if (required != null && (i >= guess.length() || guess.charAt(i) != required)) {
                return Optional.of(ordinal(i + 1) + " letter must be " + Character.toUpperCase(required));
            }
        }

        for (Map.Entry<Character, Integer> entry : requiredCounts.entrySet()) {
            long actualCount = guess.chars().filter(c -> c == entry.getKey()).count();
            if (actualCount < entry.getValue()) {
                return Optional.of("Guess must contain " + Character.toUpperCase(entry.getKey()));
            }
        }

        return Optional.empty();
    }

    private static String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) {
            return n + "th";
        }
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }
}
