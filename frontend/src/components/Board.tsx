import type { CSSProperties } from "react";
import type { GuessResult } from "../types/game";
import { Tile } from "./Tile";
import "./Board.css";

interface BoardProps {
  wordLength: number;
  maxGuesses: number;
  guesses: GuessResult[];
  letters: string[];
  cursor: number;
  onTileClick: (index: number) => void;
  shake: boolean;
}

// index.css's --tile-size (clamp(36px, 11.5vw, 62px)) is tuned for exactly 5
// columns. Custom puzzles can be 3-8 letters, so a longer word needs smaller
// tiles to avoid overflowing narrow viewports - scaled down proportionally
// to column count here rather than in CSS, since CSS custom properties can't
// reference their own root-level value while redefining it on the same
// element. 3-4 letter words are left at full baseline size (capped at 1x)
// rather than scaled up, since the design was never meant to go bigger than 5.
const BASELINE_COLUMNS = 5;
const BASELINE_MIN_PX = 36;
const BASELINE_VW = 11.5;
const BASELINE_MAX_PX = 62;

function tileSizeFor(wordLength: number): string {
  const scale = Math.min(1, BASELINE_COLUMNS / wordLength);
  const min = Math.round(BASELINE_MIN_PX * scale);
  const vw = +(BASELINE_VW * scale).toFixed(2);
  const max = Math.round(BASELINE_MAX_PX * scale);
  return `clamp(${min}px, ${vw}vw, ${max}px)`;
}

export function Board({ wordLength, maxGuesses, guesses, letters, cursor, onTileClick, shake }: BoardProps) {
  const boardStyle = { "--tile-size": tileSizeFor(wordLength) } as CSSProperties;
  const rows = [];

  for (let r = 0; r < maxGuesses; r++) {
    const submitted = guesses[r];
    const isCurrentRow = r === guesses.length;

    const cells = [];
    for (let c = 0; c < wordLength; c++) {
      if (submitted) {
        cells.push(
          <Tile key={c} letter={submitted.guess[c]} state={submitted.results[c]} delay={c} />
        );
      } else if (isCurrentRow) {
        const letter = letters[c] ?? "";
        cells.push(
          <Tile
            key={c}
            letter={letter}
            state={letter ? "filled" : "empty"}
            isCursor={c === cursor}
            onClick={() => onTileClick(c)}
          />
        );
      } else {
        cells.push(<Tile key={c} letter="" state="empty" />);
      }
    }

    rows.push(
      <div key={r} className={`board__row ${isCurrentRow && shake ? "board__row--shake" : ""}`}>
        {cells}
      </div>
    );
  }

  return (
    <div className="board" style={boardStyle}>
      {rows}
    </div>
  );
}
