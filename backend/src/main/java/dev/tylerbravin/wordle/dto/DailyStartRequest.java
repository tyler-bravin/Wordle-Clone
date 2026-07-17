package dev.tylerbravin.wordle.dto;

/**
 * Optional request body for {@code POST /api/game/daily/start}.
 *
 * @param hardMode whether the new session enforces Hard Mode guess constraints - defaults
 *                 to {@code false} if the request body is omitted entirely
 */
public record DailyStartRequest(boolean hardMode) {
}
