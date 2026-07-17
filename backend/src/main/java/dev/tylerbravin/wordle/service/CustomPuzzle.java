package dev.tylerbravin.wordle.service;

import java.time.Instant;
import java.util.UUID;

/**
 * A player-chosen word shared via a {@code /custom/{id}} link. Distinct from
 * {@link GameSession}: a puzzle is the reusable definition anyone with the
 * link can start a fresh attempt against, while a session is one specific
 * attempt (its own guesses, its own status).
 * <p>
 * {@code expiresAt} is chosen by the creator at creation time (see
 * {@link CustomPuzzleService}), not a fixed deployment-wide default - it's
 * what {@link RedisCustomPuzzleStore} derives the Redis TTL from, so the link
 * actually stops working once it passes rather than just being cosmetic.
 */
record CustomPuzzle(UUID id, String word, int maxGuesses, Instant createdAt, Instant expiresAt) {
}
