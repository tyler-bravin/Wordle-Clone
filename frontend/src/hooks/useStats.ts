import { useCallback, useEffect, useState } from "react";
import type { GameStatus } from "../types/game";

export interface Stats {
  gamesPlayed: number;
  gamesWon: number;
  currentStreak: number;
  maxStreak: number;
  /** Index 0..5 = wins in 1..6 guesses. */
  guessDistribution: number[];
  lastRecordedDayNumber: number | null;
}

const EMPTY_STATS: Stats = {
  gamesPlayed: 0,
  gamesWon: 0,
  currentStreak: 0,
  maxStreak: 0,
  guessDistribution: [0, 0, 0, 0, 0, 0],
  lastRecordedDayNumber: null,
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
 * For Daily, pass the backend's calendar-day `roundNumber` to `recordResult`
 * so a page reload on the same day doesn't double-count. For Endless, every
 * round has a unique bag position, so every finished game is recorded.
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
    (status: Extract<GameStatus, "WON" | "LOST">, guessCount: number, roundNumber: number) => {
      setStats((prev) => {
        if (prev.lastRecordedDayNumber === roundNumber) {
          return prev; // already recorded this round
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
          lastRecordedDayNumber: roundNumber,
        };
      });
    },
    []
  );

  return { stats, recordResult };
}
