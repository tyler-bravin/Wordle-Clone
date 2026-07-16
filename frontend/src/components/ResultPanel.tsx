import type { GameMode, GameStatus } from "../types/game";
import type { Stats } from "../hooks/useStats";
import { Definition } from "./Definition";
import { Countdown } from "./Countdown";
import "./ResultPanel.css";

interface ResultPanelProps {
  mode: GameMode;
  status: Extract<GameStatus, "WON" | "LOST">;
  answer: string;
  guessCount: number;
  stats: Stats;
  nextDailyResetAt: string | null;
  onPlayAgain: () => void;
}

export function ResultPanel({
  mode,
  status,
  answer,
  guessCount,
  stats,
  nextDailyResetAt,
  onPlayAgain,
}: ResultPanelProps) {
  const winPct = stats.gamesPlayed === 0 ? 0 : Math.round((stats.gamesWon / stats.gamesPlayed) * 100);
  const maxDistribution = Math.max(1, ...stats.guessDistribution);

  return (
    <div className="result-panel" role="status">
      <div className="result-panel__line">
        <span className="result-panel__prompt">$</span>{" "}
        {status === "WON" ? (
          <span className="result-panel__won">solved in {guessCount}/6</span>
        ) : (
          <span className="result-panel__lost">out of guesses</span>
        )}
      </div>
      <div className="result-panel__line">
        <span className="result-panel__label">answer:</span>{" "}
        <span className="result-panel__answer">{answer}</span>
      </div>

      <div className="result-panel__definition">
        <Definition word={answer} />
      </div>

      <div className="result-panel__divider" />

      <p className="result-panel__heading">
        <span className="result-panel__prompt">$</span> cat {mode.toLowerCase()}_stats.log
      </p>
      <div className="result-panel__grid">
        <div>
          <span className="result-panel__stat">{stats.gamesPlayed}</span>
          <span className="result-panel__stat-label">played</span>
        </div>
        <div>
          <span className="result-panel__stat">{winPct}</span>
          <span className="result-panel__stat-label">win %</span>
        </div>
        <div>
          <span className="result-panel__stat">{stats.currentStreak}</span>
          <span className="result-panel__stat-label">streak</span>
        </div>
        <div>
          <span className="result-panel__stat">{stats.maxStreak}</span>
          <span className="result-panel__stat-label">max streak</span>
        </div>
      </div>

      <div className="result-panel__distribution">
        {stats.guessDistribution.map((count, i) => (
          <div key={i} className="dist-row">
            <span className="dist-row__label">{i + 1}</span>
            <div className="dist-row__track">
              <div
                className={`dist-row__bar ${
                  status === "WON" && guessCount === i + 1 ? "dist-row__bar--current" : ""
                }`}
                style={{ width: `${(count / maxDistribution) * 100}%` }}
              >
                <span className="dist-row__count">{count}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {mode === "ENDLESS" ? (
        <button className="result-panel__action" onClick={onPlayAgain}>
          ./wordle --next
        </button>
      ) : (
        <p className="result-panel__next">
          {nextDailyResetAt ? (
            <>
              next daily word in <span className="result-panel__countdown">
                <Countdown target={nextDailyResetAt} />
              </span>
            </>
          ) : (
            "next daily word in ~24h"
          )}
        </p>
      )}
    </div>
  );
}
