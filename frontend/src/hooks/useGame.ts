import { useCallback, useEffect, useRef, useState } from "react";
import { ApiRequestError, gameApi } from "../api/client";
import type { GameMode, GameState } from "../types/game";

const DAILY_GAME_ID_KEY = "wordle-daily-game-id-v1";
const ENDLESS_GAME_ID_KEY = "wordle-endless-game-id-v1";
const ENDLESS_PLAYER_ID_KEY = "wordle-endless-player-id-v1";

interface EndlessBagInfo {
  wordsRemainingInBag: number;
  totalWordsInBag: number;
}

function makeEmptyLetters(wordLength: number): string[] {
  return Array.from({ length: wordLength }, () => "");
}

/**
 * Drives a single mode's game session: bootstrapping/resuming on mount,
 * tracking the in-progress guess, and submitting guesses to the backend.
 *
 * The in-progress guess is a fixed-length array with a cursor, not a plain
 * string, so a letter can be typed into any position independently - see
 * `skip`/`moveCursorTo` below. This is what lets you leave a gap for a
 * letter you're unsure of and come back to it, rather than always filling
 * strictly left to right.
 *
 * Daily resumes by gameId alone (there's only ever one "current" daily game).
 * Endless additionally persists a playerId so "Play again" and page reloads
 * keep drawing from the same no-repeat shuffle bag rather than starting a new one.
 */
export function useGame(mode: GameMode, onFinished: (game: GameState) => void) {
  const [game, setGame] = useState<GameState | null>(null);
  const [endlessBag, setEndlessBag] = useState<EndlessBagInfo | null>(null);
  const [letters, setLetters] = useState<string[]>([]);
  const [cursor, setCursor] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [shake, setShake] = useState(false);
  const errorTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  const onFinishedRef = useRef(onFinished);
  onFinishedRef.current = onFinished;

  const gameIdKey = mode === "DAILY" ? DAILY_GAME_ID_KEY : ENDLESS_GAME_ID_KEY;

  const showError = useCallback((message: string) => {
    setError(message);
    setShake(true);
    if (errorTimeout.current) clearTimeout(errorTimeout.current);
    errorTimeout.current = setTimeout(() => {
      setError(null);
      setShake(false);
    }, 1600);
  }, []);

  const resetInput = useCallback((wordLength: number) => {
    setLetters(makeEmptyLetters(wordLength));
    setCursor(0);
  }, []);

  const startFreshDaily = useCallback(async () => {
    const fresh = await gameApi.startDaily();
    localStorage.setItem(DAILY_GAME_ID_KEY, fresh.gameId);
    setGame(fresh);
    setEndlessBag(null);
    resetInput(fresh.wordLength);
  }, [resetInput]);

  const startFreshEndless = useCallback(async () => {
    const savedPlayerId = localStorage.getItem(ENDLESS_PLAYER_ID_KEY);
    const session = await gameApi.startEndless(savedPlayerId);
    localStorage.setItem(ENDLESS_PLAYER_ID_KEY, session.playerId);
    localStorage.setItem(ENDLESS_GAME_ID_KEY, session.game.gameId);
    setGame(session.game);
    setEndlessBag({
      wordsRemainingInBag: session.wordsRemainingInBag,
      totalWordsInBag: session.totalWordsInBag,
    });
    resetInput(session.game.wordLength);
  }, [resetInput]);

  const bootstrap = useCallback(async () => {
    setLoading(true);
    try {
      const savedGameId = localStorage.getItem(gameIdKey);
      if (savedGameId) {
        const resumed = await gameApi.getGame(savedGameId);
        setGame(resumed);
        // Bag progress isn't returned by the plain get-state endpoint, so it's
        // only shown once a fresh /endless/start response provides it.
        setEndlessBag(null);
        resetInput(resumed.wordLength);
        if (resumed.status !== "IN_PROGRESS") {
          onFinishedRef.current(resumed);
        }
        return;
      }
      throw new ApiRequestError("no saved game", 404);
    } catch {
      if (mode === "DAILY") {
        await startFreshDaily();
      } else {
        await startFreshEndless();
      }
    } finally {
      setLoading(false);
    }
  }, [gameIdKey, mode, startFreshDaily, startFreshEndless, resetInput]);

  useEffect(() => {
    bootstrap();
  }, [bootstrap]);

  const startNewGame = useCallback(async () => {
    setLoading(true);
    try {
      if (mode === "DAILY") {
        await startFreshDaily();
      } else {
        await startFreshEndless();
      }
    } finally {
      setLoading(false);
    }
  }, [mode, startFreshDaily, startFreshEndless]);

  const typeLetter = useCallback(
    (letter: string) => {
      if (!game || game.status !== "IN_PROGRESS") return;
      setLetters((prev) => {
        const next = [...prev];
        next[cursor] = letter.toLowerCase();
        return next;
      });
      setCursor((prev) => Math.min(prev + 1, game.wordLength - 1));
    },
    [game, cursor]
  );

  /** Moves the cursor forward without touching the current slot - lets you
   *  leave a gap for a letter you're unsure of and fill it in later. */
  const skip = useCallback(() => {
    if (!game || game.status !== "IN_PROGRESS") return;
    setCursor((prev) => Math.min(prev + 1, game.wordLength - 1));
  }, [game]);

  /** Jumps the cursor directly to a tile - used for clicking/tapping a
   *  specific letter to edit it, without backspacing through everything after it. */
  const moveCursorTo = useCallback(
    (index: number) => {
      if (!game || game.status !== "IN_PROGRESS") return;
      setCursor(Math.max(0, Math.min(index, game.wordLength - 1)));
    },
    [game]
  );

  const backspace = useCallback(() => {
    if (!game || game.status !== "IN_PROGRESS") return;
    // If the current slot has a letter, clear just that one. Otherwise (it's
    // already blank - e.g. you skipped past it) step back and clear the
    // previous slot instead, matching what backspace does in a normal text input.
    const hasLetterAtCursor = letters[cursor] !== "";
    const targetIndex = hasLetterAtCursor ? cursor : Math.max(cursor - 1, 0);

    setLetters((prev) => {
      const next = [...prev];
      next[targetIndex] = "";
      return next;
    });
    setCursor(targetIndex);
  }, [game, cursor, letters]);

  const submitGuess = useCallback(async () => {
    if (!game || game.status !== "IN_PROGRESS") return;
    if (letters.length !== game.wordLength || letters.some((l) => l === "")) {
      showError("Not enough letters");
      return;
    }
    const guess = letters.join("");
    try {
      const updated = await gameApi.submitGuess(game.gameId, guess);
      setGame(updated);
      resetInput(updated.wordLength);
      if (updated.status !== "IN_PROGRESS") {
        onFinishedRef.current(updated);
      }
    } catch (err) {
      if (err instanceof ApiRequestError) {
        showError(err.status === 400 ? "Not in word list" : err.message);
      } else {
        showError("Something went wrong - try again");
      }
    }
  }, [game, letters, showError, resetInput]);

  return {
    game,
    endlessBag,
    letters,
    cursor,
    loading,
    error,
    shake,
    typeLetter,
    skip,
    moveCursorTo,
    backspace,
    submitGuess,
    startNewGame,
  };
}
