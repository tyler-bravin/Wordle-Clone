import type { GameMode } from "../types/game";
import { useGame } from "../hooks/useGame";
import { useStats } from "../hooks/useStats";
import { Header } from "./Header";
import { Board } from "./Board";
import { Keyboard } from "./Keyboard";
import { Toast } from "./Toast";
import { ResultPanel } from "./ResultPanel";
import { ConnectionError } from "./ConnectionError";

const STATS_KEYS: Record<GameMode, string> = {
  DAILY: "wordle-stats-daily-v1",
  ENDLESS: "wordle-stats-endless-v1",
  CUSTOM: "wordle-stats-custom-v1",
};

// Matches the backend's fixed wordle.word-length/max-guesses config. Used only
// to size the board for the brief window before a game has loaded, so it
// occupies the same footprint whether or not `game` has arrived yet - without
// this, the board briefly disappears on every mode switch (its area unmounts
// then remounts once the new game loads) and the keyboard visibly jumps up
// and back down around it.
const DEFAULT_WORD_LENGTH = 5;
const DEFAULT_MAX_GUESSES = 6;

interface GameProps {
  mode: GameMode;
  onModeChange: (mode: GameMode) => void;
  /** Required when `mode === "CUSTOM"` - the id from the shared `/custom/{puzzleId}` link. */
  puzzleId?: string;
  /** Omit to hide the "[+ custom]" tab - e.g. while already viewing a Custom puzzle. */
  onCreateCustom?: () => void;
  /** Preference applied to the next fresh Daily/Endless start - ignored for CUSTOM,
   *  whose hard-mode setting comes from the puzzle itself. */
  hardMode: boolean;
  onToggleHardMode: () => void;
}

export function Game({ mode, onModeChange, puzzleId, onCreateCustom, hardMode, onToggleHardMode }: GameProps) {
  const { stats, recordResult } = useStats(STATS_KEYS[mode]);
  const {
    game,
    endlessBag,
    letters,
    cursor,
    loading,
    error,
    notice,
    bootstrapError,
    shake,
    typeLetter,
    skip,
    stepCursorLeft,
    moveCursorTo,
    backspace,
    submitGuess,
    startNewGame,
    retryBootstrap,
    setHardModeForCurrentGame,
  } = useGame(
    mode,
    (finished) => {
      if (finished.status === "WON" || finished.status === "LOST") {
        recordResult(finished.status, finished.guesses.length, finished.gameId);
      }
    },
    puzzleId,
    hardMode
  );

  // Reflects the *active session's* actual setting, not the header toggle's
  // current position - those can differ if the toggle changed after this
  // game started (it only takes effect on the next fresh game), so this is
  // the only reliable "is hard mode actually on right now" indicator.
  const hardModeSuffix = game?.hardMode ? " · hard mode" : "";

  // Flips the persisted preference (always, for the next fresh game) and also
  // tries to apply it to the game already on screen - see setHardModeForCurrentGame's
  // Javadoc-style comment for when that's still possible.
  const handleToggleHardMode = () => {
    const next = !hardMode;
    onToggleHardMode();
    void setHardModeForCurrentGame(next);
  };

  const statusLine = bootstrapError
    ? "connection error"
    : loading || !game
    ? "loading..."
    : mode === "DAILY"
      ? `day #${game.roundNumber} · ${game.guesses.length}/${game.maxGuesses} guesses${hardModeSuffix}`
      : mode === "CUSTOM"
        ? `${game.guesses.length}/${game.maxGuesses} guesses${hardModeSuffix}`
        : endlessBag
          ? `word ${endlessBag.totalWordsInBag - endlessBag.wordsRemainingInBag}/${endlessBag.totalWordsInBag} this cycle${hardModeSuffix}`
          : `round #${game.roundNumber}${hardModeSuffix}`;

  const finished = game && (game.status === "WON" || game.status === "LOST");

  return (
    <div className="terminal">
      <Header
        mode={mode}
        onModeChange={onModeChange}
        statusLine={statusLine}
        onCreateCustom={onCreateCustom}
        hardMode={mode === "CUSTOM" ? undefined : hardMode}
        onToggleHardMode={mode === "CUSTOM" ? undefined : handleToggleHardMode}
      />

      <div className="game__board-area">
        <Toast message={error ?? notice} />
        <Board
          wordLength={game?.wordLength ?? DEFAULT_WORD_LENGTH}
          maxGuesses={game?.maxGuesses ?? DEFAULT_MAX_GUESSES}
          guesses={game?.guesses ?? []}
          letters={letters}
          cursor={cursor}
          onTileClick={moveCursorTo}
          shake={shake}
        />
      </div>

      {bootstrapError ? (
        <div className="game__keyboard-area">
          <ConnectionError onRetry={retryBootstrap} />
        </div>
      ) : finished && game.answer ? (
        <ResultPanel
          mode={mode}
          status={game.status as "WON" | "LOST"}
          answer={game.answer}
          guessCount={game.guesses.length}
          maxGuesses={game.maxGuesses}
          stats={stats}
          nextDailyResetAt={game.nextDailyResetAt}
          onPlayAgain={startNewGame}
        />
      ) : (
        <div className="game__keyboard-area">
          <Keyboard
            guesses={game?.guesses ?? []}
            onLetter={typeLetter}
            onEnter={submitGuess}
            onBackspace={backspace}
            onSkip={skip}
            onArrowLeft={stepCursorLeft}
            disabled={!game || loading}
          />
        </div>
      )}
    </div>
  );
}
