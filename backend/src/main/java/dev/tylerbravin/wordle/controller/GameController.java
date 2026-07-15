package dev.tylerbravin.wordle.controller;

import dev.tylerbravin.wordle.dto.EndlessSessionResponse;
import dev.tylerbravin.wordle.dto.EndlessStartRequest;
import dev.tylerbravin.wordle.dto.GameStateResponse;
import dev.tylerbravin.wordle.dto.GuessRequest;
import dev.tylerbravin.wordle.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API for both game modes. Daily and Endless games share the same
 * get-state and submit-guess endpoints once created, since a {@code gameId}
 * fully identifies a session regardless of which mode started it.
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Starts a fresh game for today's shared Daily word.
     *
     * @return the new game's state, with a {@code gameId} to track it by
     */
    @PostMapping("/daily/start")
    public ResponseEntity<GameStateResponse> startDailyGame() {
        return ResponseEntity.ok(gameService.startDailyGame());
    }

    /**
     * Starts (or continues) an Endless session. Pass the {@code playerId} from a
     * previous response to keep drawing from the same no-repeat shuffle bag;
     * omit it to start a brand new one.
     *
     * @param request optional body carrying a previously issued {@code playerId}
     * @return the new round's state plus the {@code playerId} and bag progress to persist
     */
    @PostMapping("/endless/start")
    public ResponseEntity<EndlessSessionResponse> startEndlessGame(
            @RequestBody(required = false) EndlessStartRequest request
    ) {
        String playerId = request == null ? null : request.playerId();
        return ResponseEntity.ok(gameService.startEndlessGame(playerId));
    }

    /**
     * Fetches current state for an in-progress or finished game, e.g. after a page reload.
     *
     * @param gameId id of an existing game session
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameStateResponse> getGame(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getGame(gameId));
    }

    /**
     * Submits a guess for the given game and returns the updated state.
     *
     * @param gameId  id of an in-progress game session
     * @param request body containing the 5-letter guess
     */
    @PostMapping("/{gameId}/guess")
    public ResponseEntity<GameStateResponse> submitGuess(
            @PathVariable UUID gameId,
            @Valid @RequestBody GuessRequest request
    ) {
        return ResponseEntity.ok(gameService.submitGuess(gameId, request.guess()));
    }
}
