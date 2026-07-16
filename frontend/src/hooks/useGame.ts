import { useCallback, useEffect, useRef, useState } from "react";
import { ApiRequestError, gameApi } from "../api/client";
import { sound } from "../lib/sound";
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

// Retried with backoff mainly to ride out a backend redeploy: the frontend
// container can start serving before the backend one is actually ready to
// accept connections, so the very first request after a deploy can fail for
// reasons that resolve themselves within a few seconds.
const BOOTSTRAP_RETRY_DELAYS_MS = [1000, 2000, 4000, 8000];

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** Retries `attempt` after each delay in `delaysMs`, throwing the last error once none remain. */
async function withRetries<T>(attempt: () => Promise<T>, delaysMs: number[]): Promise<T> {
  try {
    return await attempt();
  } catch (err) {
    if (delaysMs.length === 0) throw err;
    await delay(delaysMs[0]);
    return withRetries(attempt, delaysMs.slice(1));
  }
}

// Must match Tile.css's transition duration and Tile.tsx's stagger multiplier -
// kept here as named constants so the reveal-tone timing below has one obvious
// place to update if the flip's pacing ever changes.
const TILE_FLIP_DURATION_MS = 650;
const TILE_FLIP_STAGGER_MS = 240;

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
  const [bootstrapError, setBootstrapError] = useState(false);
  const [shake, setShake] = useState(false);
  const errorTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  const onFinishedRef = useRef(onFinished);
  onFinishedRef.current = onFinished;

  const gameIdKey = mode === "DAILY" ? DAILY_GAME_ID_KEY : ENDLESS_GAME_ID_KEY;

  const showError = useCallback((message: string) => {
    setError(message);
    setShake(true);
    sound.error();
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

  const startFresh = useCallback(async () => {
    if (mode === "DAILY") {
      await startFreshDaily();
    } else {
      await startFreshEndless();
    }
  }, [mode, startFreshDaily, startFreshEndless]);

  /** Resumes the saved game if there is one and it still exists server-side, otherwise starts fresh. */
  const resumeOrStart = useCallback(async () => {
    const savedGameId = localStorage.getItem(gameIdKey);
    if (savedGameId) {
      try {
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
      } catch (err) {
        // A 404 genuinely means this game is gone (e.g. yesterday's Daily
        // gameId) - starting fresh is correct. Anything else (network error,
        // 5xx) doesn't tell us that, so it's rethrown for the retry loop
        // below rather than treated as "start a new game" - retrying the
        // actual resume is what should happen if this was just the backend
        // not being ready yet.
        if (!(err instanceof ApiRequestError) || err.status !== 404) {
          throw err;
        }
      }
    }
    await startFresh();
  }, [gameIdKey, startFresh, resetInput]);

  const bootstrap = useCallback(async () => {
    setLoading(true);
    setBootstrapError(false);
    try {
      await withRetries(resumeOrStart, BOOTSTRAP_RETRY_DELAYS_MS);
    } catch {
      // Most likely cause here is a redeploy in progress - the frontend can
      // start serving before the backend is actually ready to accept
      // connections. Surfaced to the player as a retry prompt rather than
      // left as a silently empty board, per BOOTSTRAP_RETRY_DELAYS_MS above.
      setBootstrapError(true);
    } finally {
      setLoading(false);
    }
  }, [resumeOrStart]);

  useEffect(() => {
    bootstrap();
  }, [bootstrap]);

  const startNewGame = useCallback(async () => {
    setLoading(true);
    try {
      await startFresh();
    } finally {
      setLoading(false);
    }
  }, [startFresh]);

  const typeLetter = useCallback(
    (letter: string) => {
      if (!game || game.status !== "IN_PROGRESS") return;
      sound.keyPress();
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
   *  leave a gap for a letter you're unsure of and fill it in later. Also
   *  used directly as the right-arrow-key handler, since "move forward
   *  without typing" is exactly the same action either way. */
  const skip = useCallback(() => {
    if (!game || game.status !== "IN_PROGRESS") return;
    sound.skip();
    setCursor((prev) => Math.min(prev + 1, game.wordLength - 1));
  }, [game]);

  /** Moves the cursor one slot back without touching any letters - the
   *  left-arrow-key mirror of skip(). */
  const stepCursorLeft = useCallback(() => {
    if (!game || game.status !== "IN_PROGRESS") return;
    sound.skip();
    setCursor((prev) => Math.max(prev - 1, 0));
  }, [game]);

  /** Jumps the cursor directly to a tile - used for clicking/tapping a
   *  specific letter to edit it, without backspacing through everything after it. */
  const moveCursorTo = useCallback(
    (index: number) => {
      if (!game || game.status !== "IN_PROGRESS") return;
      sound.skip();
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

    sound.backspace();
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

      // Each tile's color only actually swaps at the midpoint of its flip
      // (see Tile.css/Tile.tsx) - half TILE_FLIP_DURATION_MS into its own
      // flip, offset by its column's stagger delay. Timing the tone to that
      // same instant, rather than to when the guess is submitted, is what
      // makes it read as "this tile just revealed" instead of a burst of
      // sound before anything's visibly happened.
      const latestGuess = updated.guesses[updated.guesses.length - 1];
      latestGuess.results.forEach((result, i) => {
        sound.tileReveal(result, i * TILE_FLIP_STAGGER_MS + TILE_FLIP_DURATION_MS / 2);
      });

      if (updated.status === "WON" || updated.status === "LOST") {
        const cascadeEndMs =
          (latestGuess.results.length - 1) * TILE_FLIP_STAGGER_MS + TILE_FLIP_DURATION_MS + 150;
        setTimeout(() => {
          if (updated.status === "WON") sound.win();
          else sound.lose();
        }, cascadeEndMs);
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
    bootstrapError,
    shake,
    typeLetter,
    skip,
    stepCursorLeft,
    moveCursorTo,
    backspace,
    submitGuess,
    startNewGame,
    retryBootstrap: bootstrap,
  };
}
