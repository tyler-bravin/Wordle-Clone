package dev.tylerbravin.wordle.controller;

import dev.tylerbravin.wordle.dto.DailyStartRequest;
import dev.tylerbravin.wordle.dto.EndlessSessionResponse;
import dev.tylerbravin.wordle.dto.EndlessStartRequest;
import dev.tylerbravin.wordle.dto.GameStateResponse;
import dev.tylerbravin.wordle.dto.GuessRequest;
import dev.tylerbravin.wordle.dto.SetHardModeRequest;
import dev.tylerbravin.wordle.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
     * @param request optional body carrying whether to enforce Hard Mode - defaults to
     *                {@code false} if the body is omitted entirely
     * @return the new game's state, with a {@code gameId} to track it by
     */
    @PostMapping("/daily/start")
    public ResponseEntity<GameStateResponse> startDailyGame(
            @RequestBody(required = false) DailyStartRequest request
    ) {
        boolean hardMode = request != null && request.hardMode();
        return ResponseEntity.ok(gameService.startDailyGame(hardMode));
    }

    /**
     * Starts (or continues) an Endless session. Pass the {@code playerId} from a
     * previous response to keep drawing from the same no-repeat shuffle bag;
     * omit it to start a brand new one.
     *
     * @param request optional body carrying a previously issued {@code playerId} and
     *                whether to enforce Hard Mode - both default to "none"/{@code false}
     *                if the body is omitted entirely
     * @return the new round's state plus the {@code playerId} and bag progress to persist
     */
    @PostMapping("/endless/start")
    public ResponseEntity<EndlessSessionResponse> startEndlessGame(
            @RequestBody(required = false) EndlessStartRequest request
    ) {
        String playerId = request == null ? null : request.playerId();
        boolean hardMode = request != null && request.hardMode();
        return ResponseEntity.ok(gameService.startEndlessGame(playerId, hardMode));
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

    /**
     * Changes Hard Mode on an existing DAILY/ENDLESS session - only allowed before the first
     * guess is made, so the titlebar toggle can affect the game already on screen instead of
     * only the next fresh one. See {@link GameService#setHardMode}.
     *
     * @param gameId  id of an existing game session
     * @param request body carrying the new Hard Mode setting
     */
    @PatchMapping("/{gameId}/hard-mode")
    public ResponseEntity<GameStateResponse> setHardMode(
            @PathVariable UUID gameId,
            @RequestBody SetHardModeRequest request
    ) {
        return ResponseEntity.ok(gameService.setHardMode(gameId, request.hardMode()));
    }
}
