package dev.tylerbravin.wordle.controller;

import dev.tylerbravin.wordle.dto.CreateCustomPuzzleRequest;
import dev.tylerbravin.wordle.dto.CreateCustomPuzzleResponse;
import dev.tylerbravin.wordle.dto.GameStateResponse;
import dev.tylerbravin.wordle.service.CustomPuzzleService;
import dev.tylerbravin.wordle.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API for creating and starting Custom puzzles. Once a session exists,
 * {@code GET /api/game/{gameId}} and {@code POST /api/game/{gameId}/guess}
 * (see {@link GameController}) already work for it like any other mode -
 * a gameId fully identifies a session regardless of which mode started it.
 */
@RestController
@RequestMapping("/api/custom")
public class CustomPuzzleController {

    private final CustomPuzzleService customPuzzleService;
    private final GameService gameService;

    public CustomPuzzleController(CustomPuzzleService customPuzzleService, GameService gameService) {
        this.customPuzzleService = customPuzzleService;
        this.gameService = gameService;
    }

    /**
     * Validates and stores a new Custom puzzle.
     *
     * @param request the word to guess, how many guesses to allow, and how long the link stays playable
     * @return the new puzzle's id, for building a shareable {@code /custom/{id}} link
     */
    @PostMapping
    public ResponseEntity<CreateCustomPuzzleResponse> createPuzzle(
            @Valid @RequestBody CreateCustomPuzzleRequest request
    ) {
        UUID puzzleId = customPuzzleService.createPuzzle(
                request.word(), request.maxGuesses(), request.expiresInHours(), request.hardMode());
        return ResponseEntity.ok(new CreateCustomPuzzleResponse(puzzleId));
    }

    /**
     * Starts a fresh attempt at an existing Custom puzzle. Anyone with the
     * link gets their own independent session.
     *
     * @param puzzleId id of a previously created puzzle
     * @return the new session's state, with a {@code gameId} to track it by
     */
    @PostMapping("/{puzzleId}/start")
    public ResponseEntity<GameStateResponse> startCustomGame(@PathVariable UUID puzzleId) {
        return ResponseEntity.ok(gameService.startCustomGame(puzzleId));
    }
}
