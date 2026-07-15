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

/**
 * Drives a single mode's game session: bootstrapping/resuming on mount,
 * tracking the in-progress guess, and submitting guesses to the backend.
 *
 * Daily resumes by gameId alone (there's only ever one "current" daily game).
 * Endless additionally persists a playerId so "Play again" and page reloads
 * keep drawing from the same no-repeat shuffle bag rather than starting a new one.
 */
export function useGame(mode: GameMode, onFinished: (game: GameState) => void) {
  const [game, setGame] = useState<GameState | null>(null);
  const [endlessBag, setEndlessBag] = useState<EndlessBagInfo | null>(null);
  const [currentGuess, setCurrentGuess] = useState("");
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

  const startFreshDaily = useCallback(async () => {
    const fresh = await gameApi.startDaily();
    localStorage.setItem(DAILY_GAME_ID_KEY, fresh.gameId);
    setGame(fresh);
    setEndlessBag(null);
  }, []);

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
  }, []);

  const bootstrap = useCallback(async () => {
    setLoading(true);
    setCurrentGuess("");
    try {
      const savedGameId = localStorage.getItem(gameIdKey);
      if (savedGameId) {
        const resumed = await gameApi.getGame(savedGameId);
        setGame(resumed);
        // Bag progress isn't returned by the plain get-state endpoint, so it's
        // only shown once a fresh /endless/start response provides it.
        setEndlessBag(null);
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
  }, [gameIdKey, mode, startFreshDaily, startFreshEndless]);

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
      setCurrentGuess("");
    } finally {
      setLoading(false);
    }
  }, [mode, startFreshDaily, startFreshEndless]);

  const typeLetter = useCallback(
    (letter: string) => {
      if (!game || game.status !== "IN_PROGRESS") return;
      setCurrentGuess((prev) =>
        prev.length < game.wordLength ? prev + letter.toLowerCase() : prev
      );
    },
    [game]
  );

  const backspace = useCallback(() => {
    setCurrentGuess((prev) => prev.slice(0, -1));
  }, []);

  const submitGuess = useCallback(async () => {
    if (!game || game.status !== "IN_PROGRESS") return;
    if (currentGuess.length !== game.wordLength) {
      showError("Not enough letters");
      return;
    }
    try {
      const updated = await gameApi.submitGuess(game.gameId, currentGuess);
      setGame(updated);
      setCurrentGuess("");
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
  }, [game, currentGuess, showError]);

  return {
    game,
    endlessBag,
    currentGuess,
    loading,
    error,
    shake,
    typeLetter,
    backspace,
    submitGuess,
    startNewGame,
  };
}
