import type { LetterResult } from "../types/game";

const MUTED_KEY = "wordle-sound-muted-v1";

/**
 * Every sound effect in the game is synthesized on the fly with the Web
 * Audio API rather than played from audio files. That sidesteps sourcing
 * and licensing sample packs for a portfolio project, keeps the bundle
 * size untouched, and makes it trivial to keep everything deliberately
 * short and quiet - each tone here is under ~250ms with a soft attack/decay
 * envelope, not a jarring beep.
 *
 * The AudioContext is created lazily on first use (browsers block audio
 * until a user gesture happens anyway - typing a letter or clicking a key
 * already satisfies that, so there's no separate "enable audio" step).
 */
class SoundEngine {
  private ctx: AudioContext | null = null;
  private muted: boolean;

  constructor() {
    this.muted = typeof localStorage !== "undefined" && localStorage.getItem(MUTED_KEY) === "true";
  }

  isMuted(): boolean {
    return this.muted;
  }

  setMuted(muted: boolean): void {
    this.muted = muted;
    localStorage.setItem(MUTED_KEY, String(muted));
  }

  toggleMuted(): boolean {
    this.setMuted(!this.muted);
    return this.muted;
  }

  private getContext(): AudioContext | null {
    if (typeof window === "undefined") return null;
    if (!this.ctx) {
      const AudioContextCtor = window.AudioContext ?? (window as typeof window & {
        webkitAudioContext?: typeof AudioContext;
      }).webkitAudioContext;
      if (!AudioContextCtor) return null;
      this.ctx = new AudioContextCtor();
    }
    if (this.ctx.state === "suspended") {
      void this.ctx.resume();
    }
    return this.ctx;
  }

  /** Plays a single soft tone. `delayMs` schedules it relative to now, for staggering. */
  private tone(
    freq: number,
    durationMs: number,
    { type = "sine", volume = 0.06, delayMs = 0 }: { type?: OscillatorType; volume?: number; delayMs?: number } = {}
  ): void {
    if (this.muted) return;
    const ctx = this.getContext();
    if (!ctx) return;

    const startAt = ctx.currentTime + delayMs / 1000;
    const durationSec = durationMs / 1000;

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.value = freq;

    // Quick fade in, exponential fade out - avoids the click/pop a hard
    // on/off transition would make, and keeps the tail short and soft.
    gain.gain.setValueAtTime(0, startAt);
    gain.gain.linearRampToValueAtTime(volume, startAt + 0.008);
    gain.gain.exponentialRampToValueAtTime(0.0001, startAt + durationSec);

    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start(startAt);
    osc.stop(startAt + durationSec + 0.02);
  }

  /** A very small, soft tick - one per keystroke. */
  keyPress(): void {
    this.tone(620, 45, { type: "sine", volume: 0.035 });
  }

  /** A soft, neutral tick for skipping a letter without filling it in - a
   *  different timbre (triangle, not sine) from keyPress/backspace so it
   *  doesn't read as "typed" or "deleted", just "moved past". */
  skip(): void {
    this.tone(460, 40, { type: "triangle", volume: 0.03 });
  }

  /** A softer, lower tick for deleting a letter. */
  backspace(): void {
    this.tone(300, 45, { type: "sine", volume: 0.03 });
  }

  /**
   * Plays the reveal tone for one tile, pitched by result so the ear gets the
   * same CORRECT > PRESENT > ABSENT signal the color does. `delayMs` should
   * match when that tile visually reveals - see the mid-flip timing note in
   * useGame's submitGuess, which mirrors Tile.css's animation timing.
   */
  tileReveal(result: LetterResult, delayMs: number): void {
    if (result === "CORRECT") {
      this.tone(660, 140, { type: "sine", volume: 0.05, delayMs });
    } else if (result === "PRESENT") {
      this.tone(440, 130, { type: "sine", volume: 0.045, delayMs });
    } else {
      this.tone(200, 110, { type: "triangle", volume: 0.03, delayMs });
    }
  }

  /** A short, bright rising arpeggio once a game is won. */
  win(): void {
    if (this.muted) return;
    const notes = [523.25, 659.25, 783.99, 1046.5]; // C5, E5, G5, C6
    notes.forEach((freq, i) => {
      this.tone(freq, 220, { type: "sine", volume: 0.05, delayMs: i * 90 });
    });
  }

  /** A short, gentle descending pair once a game is lost - not a harsh buzzer. */
  lose(): void {
    this.tone(392, 200, { type: "sine", volume: 0.045, delayMs: 0 }); // G4
    this.tone(261.63, 260, { type: "sine", volume: 0.045, delayMs: 160 }); // C4
  }

  /** A brief low buzz for an invalid guess, synced with the board's shake. */
  error(): void {
    this.tone(150, 120, { type: "square", volume: 0.025 });
  }
}

export const sound = new SoundEngine();
