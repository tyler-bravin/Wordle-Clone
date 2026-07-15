package dev.tylerbravin.wordle.controller;

import dev.tylerbravin.wordle.dto.GameStateResponse;
import dev.tylerbravin.wordle.dto.GuessRequest;
import dev.tylerbravin.wordle.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /** Starts a fresh game for today's word and returns a new gameId to track it by. */
    @PostMapping("/start")
    public ResponseEntity<GameStateResponse> startGame() {
        return ResponseEntity.ok(gameService.startGame());
    }

    /** Fetches current state for an in-progress or finished game, e.g. after a page reload. */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameStateResponse> getGame(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getGame(gameId));
    }

    /** Submits a guess for the given game and returns the updated state. */
    @PostMapping("/{gameId}/guess")
    public ResponseEntity<GameStateResponse> submitGuess(
            @PathVariable UUID gameId,
            @Valid @RequestBody GuessRequest request
    ) {
        return ResponseEntity.ok(gameService.submitGuess(gameId, request.guess()));
    }
}
