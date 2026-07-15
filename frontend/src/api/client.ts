import type { ApiError, EndlessSessionState, GameState, WordDefinition } from "../types/game";

// Falls back to the local backend for dev. In production this is
// overridden by a real build-time environment variable, which always
// takes priority over this default - see README.md for how that's set.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiRequestError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiError | null;
    throw new ApiRequestError(
      body?.message ?? `Request failed with status ${response.status}`,
      response.status
    );
  }

  return response.json() as Promise<T>;
}

export const gameApi = {
  startDaily: () => request<GameState>("/api/game/daily/start", { method: "POST" }),

  /** Pass a previously issued playerId to keep drawing from the same shuffle bag. */
  startEndless: (playerId?: string | null) =>
    request<EndlessSessionState>("/api/game/endless/start", {
      method: "POST",
      body: JSON.stringify({ playerId: playerId ?? null }),
    }),

  getGame: (gameId: string) => request<GameState>(`/api/game/${gameId}`),

  submitGuess: (gameId: string, guess: string) =>
    request<GameState>(`/api/game/${gameId}/guess`, {
      method: "POST",
      body: JSON.stringify({ guess }),
    }),
};

export const dictionaryApi = {
  getDefinition: (word: string) => request<WordDefinition>(`/api/dictionary/${word}`),
};
