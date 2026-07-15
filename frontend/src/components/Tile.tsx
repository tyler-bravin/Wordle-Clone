import type { LetterResult } from "../types/game";
import "./Tile.css";

interface TileProps {
  letter: string;
  state: LetterResult | "empty" | "typing";
  /** Reveal animation delay index within the row, in tile positions. */
  delay?: number;
}

const STATE_CLASS: Record<string, string> = {
  CORRECT: "tile--correct",
  PRESENT: "tile--present",
  ABSENT: "tile--absent",
  empty: "tile--empty",
  typing: "tile--typing",
};

export function Tile({ letter, state, delay = 0 }: TileProps) {
  const isRevealed = state === "CORRECT" || state === "PRESENT" || state === "ABSENT";

  return (
    <div
      className={`tile ${STATE_CLASS[state]} ${isRevealed ? "tile--reveal" : ""}`}
      style={isRevealed ? { animationDelay: `${delay * 180}ms` } : undefined}
    >
      <span className="tile__letter">{letter}</span>
    </div>
  );
}
