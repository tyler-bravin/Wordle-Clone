package dev.tylerbravin.wordle.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Plain in-memory {@link CustomPuzzleStore} test double - no real Redis needed for unit tests. */
class InMemoryCustomPuzzleStore implements CustomPuzzleStore {

    private final Map<UUID, CustomPuzzle> puzzles = new ConcurrentHashMap<>();

    @Override
    public void save(CustomPuzzle puzzle) {
        puzzles.put(puzzle.id(), puzzle);
    }

    @Override
    public Optional<CustomPuzzle> find(UUID puzzleId) {
        return Optional.ofNullable(puzzles.get(puzzleId));
    }
}
