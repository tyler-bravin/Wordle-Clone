import { useState } from "react";
import type { GameMode } from "../types/game";
import { sound } from "../lib/sound";
import "./Header.css";

interface HeaderProps {
  mode: GameMode;
  onModeChange: (mode: GameMode) => void;
  statusLine: string;
  /** Omit to hide the "[+ custom]" tab - e.g. while already viewing a Custom puzzle. */
  onCreateCustom?: () => void;
  /** True while the Custom-puzzle creation form is showing - overrides the tabs
   *  regardless of `mode`, since creating isn't itself a playable GameMode. */
  creating?: boolean;
  /** Omit both (along with the toggle they drive) while viewing a Custom puzzle -
   *  hard mode there is the creator's choice, baked into the puzzle, not a toggle
   *  the guesser controls. Reflects the *preference* for the next fresh game, not
   *  necessarily the active session's setting - see `Game.tsx`'s status line. */
  hardMode?: boolean;
  onToggleHardMode?: () => void;
}

export function Header({ mode, onModeChange, statusLine, onCreateCustom, creating, hardMode, onToggleHardMode }: HeaderProps) {
  const [muted, setMuted] = useState(() => sound.isMuted());

  return (
    <div className="terminal-header">
      <div className="terminal-header__titlebar">
        <div className="terminal-header__dots">
          <span className="dot" />
          <span className="dot" />
          <span className="dot dot--amber" />
        </div>
        <span className="terminal-header__title">wordle — zsh — 80x24</span>
        {onToggleHardMode && (
          <button
            className="terminal-header__hardmode-toggle"
            onClick={onToggleHardMode}
            aria-label={hardMode ? "Turn off Hard Mode for future games" : "Turn on Hard Mode for future games"}
            aria-pressed={hardMode}
          >
            [{hardMode ? "hard" : "normal"}]
          </button>
        )}
        <button
          className="terminal-header__sound-toggle"
          onClick={() => setMuted(sound.toggleMuted())}
          aria-label={muted ? "Unmute sound effects" : "Mute sound effects"}
          aria-pressed={muted}
        >
          [{muted ? "muted" : "sound"}]
        </button>
      </div>

      <div className="terminal-header__body">
        <div className="terminal-header__prompt">
          <span className="prompt__user">guest@tylerbravin</span>
          <span className="prompt__sep">:</span>
          <span className="prompt__path">~</span>
          <span className="prompt__dollar">$</span>{" "}
          <span className="prompt__cmd">
            {creating ? "./wordle --create-custom" : `./wordle --mode=${mode.toLowerCase()}`}
          </span>
          <span className="prompt__cursor" aria-hidden="true" />
        </div>

        <div className="mode-tabs" role="tablist" aria-label="Game mode">
          {creating ? (
            <>
              <span className="mode-tab mode-tab--active mode-tab--static">[create custom]</span>
              <button role="tab" className="mode-tab" onClick={() => onModeChange("DAILY")}>
                [back]
              </button>
            </>
          ) : mode === "CUSTOM" ? (
            <>
              <span className="mode-tab mode-tab--active mode-tab--static">[custom puzzle]</span>
              <button role="tab" className="mode-tab" onClick={() => onModeChange("DAILY")}>
                [back]
              </button>
            </>
          ) : (
            <>
              <button
                role="tab"
                aria-selected={mode === "DAILY"}
                className={`mode-tab ${mode === "DAILY" ? "mode-tab--active" : ""}`}
                onClick={() => onModeChange("DAILY")}
              >
                [daily]
              </button>
              <button
                role="tab"
                aria-selected={mode === "ENDLESS"}
                className={`mode-tab ${mode === "ENDLESS" ? "mode-tab--active" : ""}`}
                onClick={() => onModeChange("ENDLESS")}
              >
                [endless]
              </button>
              {onCreateCustom && (
                <button role="tab" className="mode-tab" onClick={onCreateCustom}>
                  [+ custom]
                </button>
              )}
            </>
          )}
        </div>

        <p className="terminal-header__status">{statusLine}</p>
      </div>
    </div>
  );
}
