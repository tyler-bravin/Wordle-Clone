import type { GameMode } from "../types/game";
import { useGame } from "../hooks/useGame";
import { useStats } from "../hooks/useStats";
import { Header } from "./Header";
import { Board } from "./Board";
import { Keyboard } from "./Keyboard";
import { Toast } from "./Toast";
import { ResultPanel } from "./ResultPanel";

const STATS_KEYS: Record<GameMode, string> = {
  DAILY: "wordle-stats-daily-v1",
  ENDLESS: "wordle-stats-endless-v1",
};

interface GameProps {
  mode: GameMode;
  onModeChange: (mode: GameMode) => void;
}

export function Game({ mode, onModeChange }: GameProps) {
  const { stats, recordResult } = useStats(STATS_KEYS[mode]);
  const { game, endlessBag, currentGuess, loading, error, shake, typeLetter, backspace, submitGuess, startNewGame } =
    useGame(mode, (finished) => {
      if (finished.status === "WON" || finished.status === "LOST") {
        recordResult(finished.status, finished.guesses.length, finished.gameId);
      }
    });

  const statusLine = loading || !game
    ? "loading..."
    : mode === "DAILY"
      ? `day #${game.roundNumber} · ${game.guesses.length}/${game.maxGuesses} guesses`
      : endlessBag
        ? `word ${endlessBag.totalWordsInBag - endlessBag.wordsRemainingInBag}/${endlessBag.totalWordsInBag} this cycle`
        : `round #${game.roundNumber}`;

  const finished = game && (game.status === "WON" || game.status === "LOST");

  return (
    <div className="terminal">
      <Header mode={mode} onModeChange={onModeChange} statusLine={statusLine} />

      <div className="game__board-area">
        <Toast message={error} />
        {game && (
          <Board
            wordLength={game.wordLength}
            maxGuesses={game.maxGuesses}
            guesses={game.guesses}
            currentGuess={currentGuess}
            shake={shake}
          />
        )}
      </div>

      {finished && game.answer ? (
        <ResultPanel
          mode={mode}
          status={game.status as "WON" | "LOST"}
          answer={game.answer}
          guessCount={game.guesses.length}
          stats={stats}
          onPlayAgain={startNewGame}
        />
      ) : (
        <div className="game__keyboard-area">
          <Keyboard
            guesses={game?.guesses ?? []}
            onLetter={typeLetter}
            onEnter={submitGuess}
            onBackspace={backspace}
            disabled={!game || loading}
          />
        </div>
      )}
    </div>
  );
}
