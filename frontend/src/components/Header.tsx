import type { GameMode } from "../types/game";
import "./Header.css";

interface HeaderProps {
  mode: GameMode;
  onModeChange: (mode: GameMode) => void;
  statusLine: string;
}

export function Header({ mode, onModeChange, statusLine }: HeaderProps) {
  return (
    <div className="terminal-header">
      <div className="terminal-header__titlebar">
        <div className="terminal-header__dots">
          <span className="dot" />
          <span className="dot" />
          <span className="dot dot--amber" />
        </div>
        <span className="terminal-header__title">wordle — zsh — 80x24</span>
      </div>

      <div className="terminal-header__body">
        <div className="terminal-header__prompt">
          <span className="prompt__user">guest@tylerbravin</span>
          <span className="prompt__sep">:</span>
          <span className="prompt__path">~</span>
          <span className="prompt__dollar">$</span>{" "}
          <span className="prompt__cmd">./wordle --mode={mode.toLowerCase()}</span>
          <span className="prompt__cursor" aria-hidden="true" />
        </div>

        <div className="mode-tabs" role="tablist" aria-label="Game mode">
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
        </div>

        <p className="terminal-header__status">{statusLine}</p>
      </div>
    </div>
  );
}
