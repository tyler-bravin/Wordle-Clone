package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.dto.LetterResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes per-letter Wordle feedback for a guess against the answer.
 *
 * Two passes are required to handle repeated letters correctly:
 *   1. Mark exact position matches as CORRECT and remove them from the pool
 *      of remaining answer letters.
 *   2. For every other letter, mark PRESENT only if the answer still has an
 *      unmatched occurrence of it, then consume that occurrence; otherwise ABSENT.
 *
 * This mirrors real Wordle behaviour: guessing a letter twice when the answer
 * only contains it once yields one PRESENT/CORRECT and one ABSENT.
 */
@Component
public class GuessEvaluator {

    public List<LetterResult> evaluate(String guess, String answer) {
        int length = answer.length();
        if (guess.length() != length) {
            throw new IllegalArgumentException("Guess must be " + length + " letters");
        }

        char[] guessChars = guess.toLowerCase().toCharArray();
        char[] answerChars = answer.toLowerCase().toCharArray();
        LetterResult[] results = new LetterResult[length];

        Map<Character, Integer> remainingLetters = new HashMap<>();

        // Pass 1: exact matches
        for (int i = 0; i < length; i++) {
            if (guessChars[i] == answerChars[i]) {
                results[i] = LetterResult.CORRECT;
            } else {
                remainingLetters.merge(answerChars[i], 1, Integer::sum);
            }
        }

        // Pass 2: present / absent
        for (int i = 0; i < length; i++) {
            if (results[i] != null) {
                continue;
            }
            char c = guessChars[i];
            int available = remainingLetters.getOrDefault(c, 0);
            if (available > 0) {
                results[i] = LetterResult.PRESENT;
                remainingLetters.put(c, available - 1);
            } else {
                results[i] = LetterResult.ABSENT;
            }
        }

        return List.of(results);
    }
}
