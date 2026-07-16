import "./ConnectionError.css";

interface ConnectionErrorProps {
  onRetry: () => void;
}

/**
 * Shown when {@code useGame}'s bootstrap couldn't reach the backend even
 * after its built-in retries - most often because a redeploy served the
 * frontend before the backend was ready to accept connections. Gives the
 * player something to act on (retry) instead of an indefinitely empty board.
 */
export function ConnectionError({ onRetry }: ConnectionErrorProps) {
  return (
    <div className="connection-error">
      <p className="connection-error__message">
        <span className="connection-error__prompt">$</span> couldn't reach the server - it may still be starting up
      </p>
      <button className="connection-error__action" onClick={onRetry}>
        retry
      </button>
    </div>
  );
}
