package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import dev.tylerbravin.wordle.dto.WordDefinitionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up a word's definition from a free external dictionary API, purely
 * for the "what did that word even mean" flourish shown after a game ends.
 * <p>
 * Deliberately <b>not</b> used anywhere on the guess-submission path: this is
 * the one place in the backend that calls out to a third party, and it only
 * ever runs once, after guessing is already over, so its latency and
 * reliability can't affect actual gameplay. If the API is slow, down, or
 * simply has no entry for a word, callers get a graceful "not found" result
 * rather than an error - see {@link #lookup}.
 * <p>
 * Results are cached in memory, since the same daily/endless answer gets
 * looked up repeatedly by different players and its definition never changes
 * within a run. The cache is unbounded, which is fine at this project's
 * scale (at most a few thousand distinct words ever get looked up) but would
 * want an eviction policy in a service with real traffic.
 */
@Service
public class DictionaryService {

    private static final Logger log = LoggerFactory.getLogger(DictionaryService.class);
    private static final int MAX_MEANINGS = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;
    private final Map<String, WordDefinitionResponse> cache = new ConcurrentHashMap<>();

    public DictionaryService(GameProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.dictionaryApiBaseUrl())
                .requestFactory(timeoutLimitedRequestFactory())
                .build();
    }

    /**
     * Looks up a definition, using the in-memory cache when available.
     *
     * @param word lowercase word to look up
     * @return a response with {@code found=true} and up to {@value MAX_MEANINGS}
     *         meanings if the API had an entry; a {@code found=false} response
     *         (never an exception) if it didn't, timed out, or errored
     */
    public WordDefinitionResponse lookup(String word) {
        return cache.computeIfAbsent(word, this::fetch);
    }

    private WordDefinitionResponse fetch(String word) {
        try {
            List<DictionaryApiEntry.Entry> entries = restClient.get()
                    .uri("/{word}", word)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DictionaryApiEntry.Entry>>() {
                    });

            return toResponse(word, entries);
        } catch (Exception e) {
            // Covers 404 (no entry), timeouts, DNS failures, malformed responses -
            // all of these should degrade to "no definition available", never a
            // 500 for the player, since this endpoint is a bonus, not core gameplay.
            log.warn("Dictionary lookup failed for '{}': {}", word, e.getMessage());
            return WordDefinitionResponse.notFound(word);
        }
    }

    WordDefinitionResponse toResponse(String word, List<DictionaryApiEntry.Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return WordDefinitionResponse.notFound(word);
        }

        String phonetic = entries.stream()
                .map(DictionaryApiEntry.Entry::phonetic)
                .filter(p -> p != null && !p.isBlank())
                .findFirst()
                .orElse(null);

        List<WordDefinitionResponse.Meaning> meanings = entries.stream()
                .filter(entry -> entry.meanings() != null)
                .flatMap(entry -> entry.meanings().stream())
                .filter(meaning -> meaning.definitions() != null && !meaning.definitions().isEmpty())
                .map(this::toFirstMeaning)
                .distinct()
                .limit(MAX_MEANINGS)
                .toList();

        if (meanings.isEmpty()) {
            return WordDefinitionResponse.notFound(word);
        }

        return new WordDefinitionResponse(word, true, phonetic, meanings);
    }

    WordDefinitionResponse.Meaning toFirstMeaning(DictionaryApiEntry.Meaning meaning) {
        DictionaryApiEntry.Definition first = meaning.definitions().get(0);
        return new WordDefinitionResponse.Meaning(meaning.partOfSpeech(), first.definition(), first.example());
    }

    private SimpleClientHttpRequestFactory timeoutLimitedRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) TIMEOUT.toMillis());
        factory.setReadTimeout((int) TIMEOUT.toMillis());
        return factory;
    }
}
