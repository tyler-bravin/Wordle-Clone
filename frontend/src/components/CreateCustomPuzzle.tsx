import { useState } from "react";
import type { FormEvent } from "react";
import { ApiRequestError, customApi } from "../api/client";
import "./CreateCustomPuzzle.css";

interface CreateCustomPuzzleProps {
  onClose: () => void;
}

const MIN_GUESSES = 3;
const MAX_GUESSES = 8;
const GUESS_OPTIONS = Array.from({ length: MAX_GUESSES - MIN_GUESSES + 1 }, (_, i) => MIN_GUESSES + i);

// Presets rather than a free-entry 1-48 field - links aren't meant to be
// permanent, but picking an exact hour count is more precision than anyone
// actually needs.
const EXPIRY_OPTIONS_HOURS = [1, 6, 12, 24, 48];
const DEFAULT_EXPIRY_HOURS = 24;

function formatExpiry(hours: number): string {
  return hours === 1 ? "1 hour" : `${hours} hours`;
}

/**
 * In-page form for minting a Custom puzzle link. Shown in place of the board
 * (see `App.tsx`) rather than as a modal, matching this app's habit of
 * swapping the terminal body's content instead of layering dialogs on top.
 */
export function CreateCustomPuzzle({ onClose }: CreateCustomPuzzleProps) {
  const [word, setWord] = useState("");
  const [maxGuesses, setMaxGuesses] = useState(6);
  const [expiresInHours, setExpiresInHours] = useState(DEFAULT_EXPIRY_HOURS);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [link, setLink] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const { puzzleId } = await customApi.create(word, maxGuesses, expiresInHours);
      setLink(`${window.location.origin}/custom/${puzzleId}`);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Something went wrong - try again");
    } finally {
      setSubmitting(false);
    }
  };

  const handleCopy = async () => {
    if (!link) return;
    try {
      await navigator.clipboard.writeText(link);
      setCopied(true);
      setTimeout(() => setCopied(false), 1600);
    } catch {
      // Clipboard permission can be denied - the link is still selectable/visible either way.
    }
  };

  if (link) {
    return (
      <div className="create-custom">
        <p className="create-custom__prompt">
          <span className="create-custom__dollar">$</span> puzzle created - share this link
        </p>
        <p className="create-custom__expiry">expires in {formatExpiry(expiresInHours)}</p>
        <div className="create-custom__link-row">
          <input
            className="create-custom__link"
            value={link}
            readOnly
            onFocus={(e) => e.target.select()}
            aria-label="Shareable puzzle link"
          />
          <button type="button" className="create-custom__copy" onClick={handleCopy}>
            {copied ? "copied" : "copy"}
          </button>
        </div>
        <div className="create-custom__actions">
          <a className="create-custom__submit create-custom__play" href={link}>
            play it
          </a>
          <button type="button" className="create-custom__back" onClick={onClose}>
            back
          </button>
        </div>
      </div>
    );
  }

  return (
    <form className="create-custom" onSubmit={handleSubmit}>
      <p className="create-custom__prompt">
        <span className="create-custom__dollar">$</span> ./wordle --create-custom
      </p>

      <label className="create-custom__field">
        <span>word</span>
        <input
          type="text"
          value={word}
          onChange={(e) => setWord(e.target.value)}
          maxLength={MAX_GUESSES}
          autoComplete="off"
          autoCapitalize="off"
          spellCheck={false}
          placeholder="e.g. crane"
          disabled={submitting}
        />
      </label>

      <label className="create-custom__field">
        <span>guesses allowed</span>
        <select
          value={maxGuesses}
          onChange={(e) => setMaxGuesses(Number(e.target.value))}
          disabled={submitting}
        >
          {GUESS_OPTIONS.map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
      </label>

      <label className="create-custom__field">
        <span>link expires in</span>
        <select
          value={expiresInHours}
          onChange={(e) => setExpiresInHours(Number(e.target.value))}
          disabled={submitting}
        >
          {EXPIRY_OPTIONS_HOURS.map((hours) => (
            <option key={hours} value={hours}>
              {formatExpiry(hours)}
            </option>
          ))}
        </select>
      </label>

      {error && <p className="create-custom__error">{error}</p>}

      <div className="create-custom__actions">
        <button type="submit" className="create-custom__submit" disabled={submitting || !word.trim()}>
          {submitting ? "creating..." : "create link"}
        </button>
        <button type="button" className="create-custom__back" onClick={onClose}>
          cancel
        </button>
      </div>
    </form>
  );
}
