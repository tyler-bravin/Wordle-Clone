package dev.tylerbravin.wordle.dto;

/**
 * Optional request body for {@code POST /api/game/endless/start}.
 *
 * @param playerId id of a previously created shuffle bag to resume, or {@code null}/absent
 *                 to create a brand new one
 */
public record EndlessStartRequest(String playerId) {
}
