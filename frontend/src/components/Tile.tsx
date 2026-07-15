import type { LetterResult } from "../types/game";
import "./Tile.css";

interface TileProps {
  letter: string;
  state: LetterResult | "empty" | "filled";
  /** Reveal animation delay index within the row, in tile positions. */
  delay?: number;
  /** Whether this is the slot the next typed letter will land in. */
  isCursor?: boolean;
  onClick?: () => void;
}

const STATE_CLASS: Record<string, string> = {
  CORRECT: "tile--reveal-correct",
  PRESENT: "tile--reveal-present",
  ABSENT: "tile--reveal-absent",
  empty: "tile--empty",
  filled: "tile--filled",
};

export function Tile({ letter, state, delay = 0, isCursor = false, onClick }: TileProps) {
  const isRevealed = state === "CORRECT" || state === "PRESENT" || state === "ABSENT";

  return (
    <div
      className={`tile ${STATE_CLASS[state]} ${isCursor ? "tile--cursor" : ""} ${onClick ? "tile--clickable" : ""}`}
      style={isRevealed ? { animationDelay: `${delay * 180}ms` } : undefined}
      onClick={onClick}
    >
      <span className="tile__letter">{letter}</span>
    </div>
  );
}
