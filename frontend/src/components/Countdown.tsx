import { useEffect, useState } from "react";

interface CountdownProps {
  target: string;
}

function formatRemaining(ms: number): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds].map((n) => String(n).padStart(2, "0")).join(":");
}

/**
 * Ticks down to `target` (an ISO instant, e.g. GameState.nextDailyResetAt) once
 * per second. Uses the browser's own clock against the server-provided target
 * rather than trusting any client-side notion of "midnight" - the backend pins
 * the actual reset to UTC (see GameService's Javadoc), so this stays correct
 * regardless of the player's own timezone.
 */
export function Countdown({ target }: CountdownProps) {
  const targetMs = new Date(target).getTime();
  const [remainingMs, setRemainingMs] = useState(() => targetMs - Date.now());

  useEffect(() => {
    setRemainingMs(targetMs - Date.now());
    const id = setInterval(() => {
      setRemainingMs(targetMs - Date.now());
    }, 1000);
    return () => clearInterval(id);
  }, [targetMs]);

  if (remainingMs <= 0) {
    return <>a new word is ready - refresh to play</>;
  }

  return <>{formatRemaining(remainingMs)}</>;
}
