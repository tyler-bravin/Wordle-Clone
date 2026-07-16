import { useEffect } from "react";
import type { GuessResult } from "../types/game";
import { computeLetterStates } from "../lib/letterStates";
import "./Keyboard.css";

interface KeyboardProps {
  guesses: GuessResult[];
  onLetter: (letter: string) => void;
  onEnter: () => void;
  onBackspace: () => void;
  onSkip: () => void;
  onArrowLeft: () => void;
  disabled: boolean;
}

const ROWS = [
  ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
  ["a", "s", "d", "f", "g", "h", "j", "k", "l"],
  ["enter", "z", "x", "c", "v", "b", "n", "m", "backspace"],
];

export function Keyboard({ guesses, onLetter, onEnter, onBackspace, onSkip, onArrowLeft, disabled }: KeyboardProps) {
  const letterStates = computeLetterStates(guesses);

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (disabled) return;
      if (e.key === "Enter") {
        onEnter();
      } else if (e.key === "Backspace") {
        onBackspace();
      } else if (e.key === " ") {
        // Space scrolls the page by default when nothing else has focus -
        // block that, since here it means "skip this letter", not "scroll".
        e.preventDefault();
        onSkip();
      } else if (e.key === "ArrowRight") {
        // Same action as skip - "move forward without typing" either way.
        e.preventDefault();
        onSkip();
      } else if (e.key === "ArrowLeft") {
        e.preventDefault();
        onArrowLeft();
      } else if (/^[a-zA-Z]$/.test(e.key)) {
        onLetter(e.key);
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [disabled, onEnter, onBackspace, onSkip, onArrowLeft, onLetter]);

  return (
    <div className="keyboard">
      {ROWS.map((row, i) => (
        <div key={i} className="keyboard__row">
          {row.map((key) => {
            if (key === "enter") {
              return (
                <button
                  key={key}
                  className="key key--wide"
                  onClick={onEnter}
                  disabled={disabled}
                  aria-label="Submit guess"
                >
                  Enter
                </button>
              );
            }
            if (key === "backspace") {
              return (
                <button
                  key={key}
                  className="key key--wide key--backspace"
                  onClick={onBackspace}
                  disabled={disabled}
                  aria-label="Delete letter"
                >
                  <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                    <path d="M22 3H7c-.69 0-1.23.35-1.59.88L0 12l5.41 8.11c.36.53.9.89 1.59.89h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2.59 12.59L18 17l-3-3-3 3-1.41-1.41L13.17 12 10.59 9.41 12 8l3 3 3-3 1.41 1.41L16.83 12l2.58 2.59z" />
                  </svg>
                </button>
              );
            }
            const state = letterStates[key];
            return (
              <button
                key={key}
                className={`key ${state ? `key--${state.toLowerCase()}` : ""}`}
                onClick={() => onLetter(key)}
                disabled={disabled}
              >
                {key}
              </button>
            );
          })}
        </div>
      ))}
    </div>
  );
}
