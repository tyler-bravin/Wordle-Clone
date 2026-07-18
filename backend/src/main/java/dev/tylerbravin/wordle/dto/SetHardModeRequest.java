package dev.tylerbravin.wordle.dto;

/**
 * Request body for {@code PATCH /api/game/{gameId}/hard-mode}.
 *
 * @param hardMode the new Hard Mode setting for this session
 */
public record SetHardModeRequest(boolean hardMode) {
}
