export type LetterResult = "CORRECT" | "PRESENT" | "ABSENT";

export type GameStatus = "IN_PROGRESS" | "WON" | "LOST";

export type GameMode = "DAILY" | "ENDLESS";

export interface GuessResult {
  guess: string;
  results: LetterResult[];
}

export interface GameState {
  gameId: string;
  mode: GameMode;
  /** Calendar day index for DAILY, or 1-based shuffle-bag position for ENDLESS. */
  roundNumber: number;
  wordLength: number;
  maxGuesses: number;
  guesses: GuessResult[];
  status: GameStatus;
  answer: string | null;
}

export interface EndlessSessionState {
  game: GameState;
  playerId: string;
  wordsRemainingInBag: number;
  totalWordsInBag: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
