import { useCallback, useEffect, useState } from "react";
import type { GameStatus } from "../types/game";

export interface Stats {
  gamesPlayed: number;
  gamesWon: number;
  currentStreak: number;
  maxStreak: number;
  /** Index 0..5 = wins in 1..6 guesses. */
  guessDistribution: number[];
  lastRecordedGameId: string | null;
}

const EMPTY_STATS: Stats = {
  gamesPlayed: 0,
  gamesWon: 0,
  currentStreak: 0,
  maxStreak: 0,
  guessDistribution: [0, 0, 0, 0, 0, 0],
  lastRecordedGameId: null,
};

function loadStats(storageKey: string): Stats {
  try {
    const raw = localStorage.getItem(storageKey);
    if (!raw) return EMPTY_STATS;
    return { ...EMPTY_STATS, ...(JSON.parse(raw) as Partial<Stats>) };
  } catch {
    return EMPTY_STATS;
  }
}

/**
 * Tracks win/loss stats in localStorage under the given key, so Daily and
 * Endless mode can keep independent stats by using two different keys.
 *
 * Deduplicates by `gameId` rather than round number. Round numbers (calendar
 * day for Daily, bag position for Endless) aren't reliable dedup keys on
 * their own: game state is in-memory only (see README's "Known
 * Simplifications"), so a backend restart mid-session can hand out a *new*
 * gameId that happens to reuse a round number a previous, differently-scored
 * game already used - e.g. replaying today's Daily word after the original
 * session was lost. Keying on gameId instead means every distinct finished
 * game gets recorded exactly once, no matter what round number it landed on.
 */
export function useStats(storageKey: string) {
  const [stats, setStats] = useState<Stats>(() => loadStats(storageKey));

  useEffect(() => {
    setStats(loadStats(storageKey));
  }, [storageKey]);

  useEffect(() => {
    localStorage.setItem(storageKey, JSON.stringify(stats));
  }, [storageKey, stats]);

  const recordResult = useCallback(
    (status: Extract<GameStatus, "WON" | "LOST">, guessCount: number, gameId: string) => {
      setStats((prev) => {
        if (prev.lastRecordedGameId === gameId) {
          return prev; // already recorded this exact game
        }

        const won = status === "WON";
        const distribution = [...prev.guessDistribution];
        if (won && guessCount >= 1 && guessCount <= 6) {
          distribution[guessCount - 1] += 1;
        }

        return {
          gamesPlayed: prev.gamesPlayed + 1,
          gamesWon: prev.gamesWon + (won ? 1 : 0),
          currentStreak: won ? prev.currentStreak + 1 : 0,
          maxStreak: won ? Math.max(prev.maxStreak, prev.currentStreak + 1) : prev.maxStreak,
          guessDistribution: distribution,
          lastRecordedGameId: gameId,
        };
      });
    },
    []
  );

  return { stats, recordResult };
}
