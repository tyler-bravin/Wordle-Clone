package dev.tylerbravin.wordle.dto;

/**
 * Returned by the Endless-mode endpoints instead of a bare {@link GameStateResponse},
 * since the client also needs to persist {@code playerId} to keep drawing from the
 * same shuffle bag across rounds.
 *
 * @param game                 the underlying game state for this round
 * @param playerId             id of this player's shuffle bag - store it and send it
 *                             back on the next {@code /api/game/endless/start} call
 * @param wordsRemainingInBag  words left before the bag reshuffles
 * @param totalWordsInBag      total size of the answer pool, for showing progress (e.g. "142 / 2315")
 */
public record EndlessSessionResponse(
        GameStateResponse game,
        String playerId,
        int wordsRemainingInBag,
        int totalWordsInBag
) {
}
