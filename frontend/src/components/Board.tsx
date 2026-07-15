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

export function Board({ wordLength, maxGuesses, guesses, letters, cursor, onTileClick, shake }: BoardProps) {
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

  return <div className="board">{rows}</div>;
}
