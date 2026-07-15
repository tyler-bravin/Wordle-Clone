import type { GuessResult, LetterResult } from "../types/game";

const PRIORITY: Record<LetterResult, number> = {
  ABSENT: 0,
  PRESENT: 1,
  CORRECT: 2,
};

/**
 * Reduces every guess into a single best-seen state per letter, e.g. if "e" was
 * PRESENT in one guess and CORRECT in a later one, it should show as CORRECT on
 * the keyboard - never downgraded by an earlier, worse result.
 */
export function computeLetterStates(guesses: GuessResult[]): Record<string, LetterResult> {
  const states: Record<string, LetterResult> = {};

  for (const { guess, results } of guesses) {
    for (let i = 0; i < guess.length; i++) {
      const letter = guess[i];
      const result = results[i];
      const existing = states[letter];
      if (!existing || PRIORITY[result] > PRIORITY[existing]) {
        states[letter] = result;
      }
    }
  }

  return states;
}
