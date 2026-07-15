import { useEffect, useState } from "react";
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

/** Classes that own the flip + mid-animation color swap (see Tile.css). */
const REVEAL_ANIMATION_CLASS: Record<string, string> = {
  CORRECT: "tile--reveal-correct",
  PRESENT: "tile--reveal-present",
  ABSENT: "tile--reveal-absent",
};

/**
 * Plain, static classes with directly-set background/border/text colors -
 * no @keyframes, no custom properties. Swapped in once the flip animation
 * completes.
 *
 * This split exists because `animation-fill-mode: forwards` isn't reliable
 * for *unregistered* CSS custom properties (the --tile-bg/--tile-border/etc.
 * technique the reveal animation uses to hide the color swap mid-flip) -
 * some browsers don't keep applying the last keyframe's custom property
 * values once the animation finishes, so the tile would flip, reveal its
 * color for an instant, then visually reset to blank. Handling "what
 * persists after the animation" with a normal class swap on `onAnimationEnd`
 * sidesteps that inconsistency entirely, since a plain class has no such
 * caveat.
 */
const FINAL_CLASS: Record<string, string> = {
  CORRECT: "tile--correct",
  PRESENT: "tile--present",
  ABSENT: "tile--absent",
};

const STATE_CLASS: Record<string, string> = {
  empty: "tile--empty",
  filled: "tile--filled",
};

export function Tile({ letter, state, delay = 0, isCursor = false, onClick }: TileProps) {
  const isRevealed = state === "CORRECT" || state === "PRESENT" || state === "ABSENT";
  const [animationDone, setAnimationDone] = useState(false);

  // A given tile cell is only ever reused for a genuinely different letter
  // when a row resets between games, at which point it should be able to
  // animate again rather than staying stuck on "done" from a previous game.
  useEffect(() => {
    setAnimationDone(false);
  }, [state, letter]);

  const classes = ["tile"];
  if (isRevealed) {
    classes.push(animationDone ? FINAL_CLASS[state] : REVEAL_ANIMATION_CLASS[state]);
  } else {
    classes.push(STATE_CLASS[state]);
  }
  if (isCursor) classes.push("tile--cursor");
  if (onClick) classes.push("tile--clickable");

  return (
    <div
      className={classes.join(" ")}
      style={isRevealed && !animationDone ? { animationDelay: `${delay * 180}ms` } : undefined}
      onClick={onClick}
      onAnimationEnd={() => {
        if (isRevealed) setAnimationDone(true);
      }}
    >
      <span className="tile__letter">{letter}</span>
    </div>
  );
}
