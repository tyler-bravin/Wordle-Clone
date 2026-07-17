export type LetterResult = "CORRECT" | "PRESENT" | "ABSENT";

export type GameStatus = "IN_PROGRESS" | "WON" | "LOST";

export type GameMode = "DAILY" | "ENDLESS" | "CUSTOM";

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
  /** ISO instant of the next UTC-midnight Daily reset. Null for ENDLESS games. */
  nextDailyResetAt: string | null;
  /** Fixed for this session's lifetime - may differ from the player's current toggle preference. */
  hardMode: boolean;
}

export interface CreateCustomPuzzleResponse {
  puzzleId: string;
}

export interface EndlessSessionState {
  game: GameState;
  playerId: string;
  wordsRemainingInBag: number;
  totalWordsInBag: number;
}

export interface WordDefinitionMeaning {
  partOfSpeech: string;
  definition: string;
  example: string | null;
}

export interface WordDefinition {
  word: string;
  found: boolean;
  phonetic: string | null;
  meanings: WordDefinitionMeaning[];
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
