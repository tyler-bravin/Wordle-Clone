import { useEffect, useState } from "react";
import type { LetterResult } from "../types/game";
import "./Tile.css";

interface TileProps {
  letter: string;
  state: LetterResult | "empty" | "filled";
  /** Stagger index within the row - each tile's flip is delayed this many
   *  positions' worth of time after the row is submitted. */
  delay?: number;
  /** Whether this is the slot the next typed letter will land in. */
  isCursor?: boolean;
  onClick?: () => void;
}

/**
 * Renders as a physical two-sided flip card (front face = unrevealed look,
 * back face = revealed color), rotated in 3D via `transform`, rather than
 * animating color via custom properties in a @keyframes block.
 *
 * That's a deliberate do-over from an earlier version: browsers don't
 * reliably persist *custom property* values set inside keyframes once
 * `animation-fill-mode: forwards` should be holding them, which showed up
 * as the reveal color flickering off right after each tile finished
 * flipping. A `transform` on the other hand is a completely ordinary,
 * fully-interpolated CSS property with no such caveat - so the only thing
 * that ever animates here is `rotateX`, and the "reveal" is really just
 * which of the two static, never-animated faces is rotated into view.
 */
export function Tile({ letter, state, delay = 0, isCursor = false, onClick }: TileProps) {
  const isRevealed = state === "CORRECT" || state === "PRESENT" || state === "ABSENT";
  const [flipped, setFlipped] = useState(false);

  useEffect(() => {
    if (!isRevealed) {
      setFlipped(false);
      return;
    }
    // Flip on the frame *after* mount, so the browser paints the unflipped
    // state first - flipping in the same commit would give the transition
    // nothing to animate from, and it'd just jump straight to the end state.
    let secondFrame = 0;
    const firstFrame = requestAnimationFrame(() => {
      secondFrame = requestAnimationFrame(() => setFlipped(true));
    });
    return () => {
      cancelAnimationFrame(firstFrame);
      cancelAnimationFrame(secondFrame);
    };
  }, [isRevealed, letter, state]);

  const frontClasses = ["tile__face", "tile__face--front"];
  if (!isRevealed) {
    frontClasses.push(state === "empty" ? "tile__face--empty" : "tile__face--filled");
  }
  if (isCursor) frontClasses.push("tile__face--cursor");

  const hostClasses = ["tile"];
  if (onClick) hostClasses.push("tile--clickable");

  return (
    <div className={hostClasses.join(" ")} onClick={onClick}>
      <div
        className={`tile__flipper ${flipped ? "tile__flipper--flipped" : ""}`}
        style={isRevealed ? { transitionDelay: `${delay * 240}ms` } : undefined}
      >
        <div className={frontClasses.join(" ")}>
          <span className="tile__letter">{letter}</span>
        </div>
        {isRevealed && (
          <div className={`tile__face tile__face--back tile__face--${state.toLowerCase()}`}>
            <span className="tile__letter">{letter}</span>
          </div>
        )}
      </div>
    </div>
  );
}
