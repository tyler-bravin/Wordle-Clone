package dev.tylerbravin.wordle.service;

import java.util.Optional;
import java.util.UUID;

/** Persists {@link CustomPuzzle} definitions, keyed by {@code puzzleId}. */
interface CustomPuzzleStore {

    void save(CustomPuzzle puzzle);

    Optional<CustomPuzzle> find(UUID puzzleId);
}
