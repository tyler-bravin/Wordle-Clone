package dev.tylerbravin.wordle.dto;

import java.util.UUID;

/**
 * Response for {@code POST /api/custom}.
 *
 * @param puzzleId id of the newly created puzzle - the frontend builds a
 *                 shareable link from this as {@code /custom/{puzzleId}}
 */
public record CreateCustomPuzzleResponse(UUID puzzleId) {
}
