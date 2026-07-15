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
                  className="key key--wide"
                  onClick={onBackspace}
                  disabled={disabled}
                  aria-label="Delete letter"
                >
                  ⌫
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
