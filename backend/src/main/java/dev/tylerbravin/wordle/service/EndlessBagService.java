package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.exception.PlayerNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backs Endless mode with a classic "shuffle bag": each player gets their own
 * randomly-ordered queue of every answer word. Words are dealt out one at a time
 * via {@link #nextWord(UUID)} and never repeat until the bag empties, at which
 * point it is reshuffled and refilled from the full pool - so across a long
 * Endless session a player sees every word before any word repeats, but the
 * order itself is unpredictable.
 * <p>
 * State is held in memory only, keyed by a per-player {@code playerId} the client
 * persists (e.g. in localStorage) so the bag survives across individual Endless
 * rounds. It does not survive a server restart, matching the same tradeoff made
 * by {@link GameService} for game sessions.
 */
@Service
public class EndlessBagService {

    /** Snapshot returned after dealing a word: the word itself plus bag progress. */
    public record DealtWord(String word, int position, int totalWords) {
    }

    private final List<String> answerPool;
    private final Map<UUID, Deque<String>> bags = new ConcurrentHashMap<>();

    public EndlessBagService(WordService wordService) {
        this.answerPool = wordService.answerPool();
    }

    /**
     * Creates a brand new shuffle bag for a new Endless player.
     *
     * @return the freshly generated playerId the client should hold onto to resume this bag
     */
    public UUID createPlayer() {
        UUID playerId = UUID.randomUUID();
        bags.put(playerId, freshBag());
        return playerId;
    }

    /**
     * @param playerId candidate id from the client
     * @return true if a bag exists for this id (i.e. it's safe to deal from)
     */
    public boolean hasPlayer(UUID playerId) {
        return bags.containsKey(playerId);
    }

    /**
     * Deals the next word from a player's bag, transparently reshuffling a fresh
     * bag if the current one has been fully dealt.
     *
     * @param playerId id of an existing bag, from {@link #createPlayer()}
     * @return the dealt word plus how far through the current cycle this player is
     * @throws PlayerNotFoundException if no bag exists for this id
     */
    public DealtWord nextWord(UUID playerId) {
        Deque<String> bag = bags.get(playerId);
        if (bag == null) {
            throw new PlayerNotFoundException(playerId);
        }
        if (bag.isEmpty()) {
            bag = freshBag();
            bags.put(playerId, bag);
        }
        String word = bag.poll();
        int totalWords = answerPool.size();
        int position = totalWords - bag.size();
        return new DealtWord(word, position, totalWords);
    }

    /**
     * @param playerId id of an existing bag
     * @return how many words remain undealt in the current cycle, or the full pool
     *         size if the bag doesn't exist yet
     */
    public int wordsRemaining(UUID playerId) {
        Deque<String> bag = bags.get(playerId);
        return bag == null ? answerPool.size() : bag.size();
    }

    public int totalWords() {
        return answerPool.size();
    }

    private Deque<String> freshBag() {
        List<String> shuffled = new ArrayList<>(answerPool);
        // Deliberately no fixed seed here - unlike the daily word, each Endless
        // cycle should be genuinely unpredictable, not just stable-but-obscure.
        Collections.shuffle(shuffled);
        return new ArrayDeque<>(shuffled);
    }
}
