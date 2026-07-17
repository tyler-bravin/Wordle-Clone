package dev.tylerbravin.wordle.dto;

/**
 * Optional request body for {@code POST /api/game/endless/start}.
 *
 * @param playerId id of a previously created shuffle bag to resume, or {@code null}/absent
 *                 to create a brand new one
 * @param hardMode whether the new round enforces Hard Mode guess constraints - defaults to
 *                 {@code false} (Java's default for a missing boolean field) if the request
 *                 body is present but omits it
 */
public record EndlessStartRequest(String playerId, boolean hardMode) {
}
