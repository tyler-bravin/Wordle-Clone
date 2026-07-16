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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up a word's definition from a free external dictionary API, purely
 * for the "what did that word even mean" flourish shown after a game ends.
 * <p>
 * Deliberately <b>not</b> used anywhere on the guess-submission path: this is
 * the one place in the backend that calls out to a third party, and it only
 * ever runs once, after guessing is already over, so its latency and
 * reliability can't affect actual gameplay. If both APIs are slow, down, or
 * simply have no entry for a word, callers get a graceful "not found" result
 * rather than an error - see {@link #lookup}.
 * <p>
 * Falls back to a second source (Wiktionary) when the primary has no entry -
 * the two have different coverage gaps (the primary tends to skip common
 * function words like "every"), so trying both catches more words than either
 * alone. Results are cached in memory, since the same daily/endless answer
 * gets looked up repeatedly by different players and its definition never
 * changes within a run. The cache is unbounded, which is fine at this
 * project's scale (at most a few thousand distinct words ever get looked up)
 * but would want an eviction policy in a service with real traffic.
 */
@Service
public class DictionaryService {

    private static final Logger log = LoggerFactory.getLogger(DictionaryService.class);
    private static final int MAX_MEANINGS = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    /**
     * Wiktionary marks senses with a usage qualifier - dated, slang, vulgar,
     * derogatory, etc. - with a {@code <span class="usage-label-sense">} wrapper,
     * but this REST endpoint renders that span empty rather than including the
     * actual qualifier text. Concretely: "zesty" has an entry reading "(of a man)
     * flamboyantly or effeminately gay" with no indication it's a dated/slang
     * sense, because the "(dated, sometimes offensive)" label got dropped. Since
     * the qualifier can't be recovered, any sense carrying this marker is skipped
     * entirely rather than risk presenting a slang/dated/vulgar sense as if it
     * were a plain, neutral one.
     */
    private static final String USAGE_LABEL_MARKER = "usage-label-sense";

    private final RestClient primaryClient;
    private final RestClient fallbackClient;
    private final Map<String, WordDefinitionResponse> cache = new ConcurrentHashMap<>();

    public DictionaryService(GameProperties properties) {
        this.primaryClient = RestClient.builder()
                .baseUrl(properties.dictionaryApiBaseUrl())
                .requestFactory(timeoutLimitedRequestFactory())
                .build();
        this.fallbackClient = RestClient.builder()
                .baseUrl(properties.fallbackDictionaryApiBaseUrl())
                // Wikimedia's REST API asks callers to identify themselves - see
                // https://meta.wikimedia.org/wiki/User-Agent_policy - untagged
                // traffic risks being rate-limited or blocked outright.
                .defaultHeader("User-Agent", "Wordle-Clone/1.0 (https://github.com/tyler-bravin/Wordle-Clone)")
                .requestFactory(timeoutLimitedRequestFactory())
                .build();
    }

    /**
     * Looks up a definition, using the in-memory cache when available.
     *
     * @param word lowercase word to look up
     * @return a response with {@code found=true} and up to {@value MAX_MEANINGS}
     *         meanings if either source had an entry; a {@code found=false}
     *         response (never an exception) if neither did
     */
    public WordDefinitionResponse lookup(String word) {
        return cache.computeIfAbsent(word, this::fetch);
    }

    private WordDefinitionResponse fetch(String word) {
        WordDefinitionResponse primary = fetchFromPrimary(word);
        return primary.found() ? primary : fetchFromFallback(word);
    }

    private WordDefinitionResponse fetchFromPrimary(String word) {
        try {
            List<DictionaryApiEntry.Entry> entries = primaryClient.get()
                    .uri("/{word}", word)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DictionaryApiEntry.Entry>>() {
                    });

            return toResponse(word, entries);
        } catch (Exception e) {
            // Covers 404 (no entry), timeouts, DNS failures, malformed responses -
            // all of these should degrade to "no definition available", never a
            // 500 for the player, since this endpoint is a bonus, not core gameplay.
            log.warn("Primary dictionary lookup failed for '{}': {}", word, e.getMessage());
            return WordDefinitionResponse.notFound(word);
        }
    }

    private WordDefinitionResponse fetchFromFallback(String word) {
        try {
            Map<String, List<WiktionaryEntry.PartOfSpeech>> entriesByLanguage = fallbackClient.get()
                    .uri("/{word}", word)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, List<WiktionaryEntry.PartOfSpeech>>>() {
                    });

            return toResponseFromWiktionary(word, entriesByLanguage);
        } catch (Exception e) {
            log.warn("Fallback dictionary lookup failed for '{}': {}", word, e.getMessage());
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

    WordDefinitionResponse toResponseFromWiktionary(
            String word, Map<String, List<WiktionaryEntry.PartOfSpeech>> entriesByLanguage) {
        List<WiktionaryEntry.PartOfSpeech> entries = entriesByLanguage == null ? null : entriesByLanguage.get("en");
        if (entries == null || entries.isEmpty()) {
            return WordDefinitionResponse.notFound(word);
        }

        List<WordDefinitionResponse.Meaning> meanings = entries.stream()
                .filter(entry -> entry.definitions() != null)
                .flatMap(entry -> entry.definitions().stream()
                        .map(definition -> toWiktionaryMeaning(entry.partOfSpeech(), definition)))
                .filter(Objects::nonNull)
                .distinct()
                .limit(MAX_MEANINGS)
                .toList();

        if (meanings.isEmpty()) {
            return WordDefinitionResponse.notFound(word);
        }

        // The definition endpoint doesn't carry pronunciation data - only the
        // primary source ever populates this.
        return new WordDefinitionResponse(word, true, null, meanings);
    }

    /**
     * @return null if the entry has no usable (non-blank, post-HTML-stripping) definition
     *         text, or if it carries a usage qualifier this API can't actually render (see
     *         {@link #USAGE_LABEL_MARKER})
     */
    private WordDefinitionResponse.Meaning toWiktionaryMeaning(String partOfSpeech, WiktionaryEntry.Definition definition) {
        String raw = definition.definition();
        if (raw != null && raw.contains(USAGE_LABEL_MARKER)) {
            return null;
        }
        String text = stripHtml(raw);
        if (text == null) {
            return null;
        }
        String example = definition.examples() == null || definition.examples().isEmpty()
                ? null
                : stripHtml(definition.examples().get(0));
        String normalizedPartOfSpeech = partOfSpeech == null ? null : partOfSpeech.toLowerCase();
        return new WordDefinitionResponse.Meaning(normalizedPartOfSpeech, text, example);
    }

    /**
     * Wiktionary's definition/example text carries embedded markup (wiki-links,
     * spans) rather than the plain text the primary source and the frontend
     * both expect - strips tags and decodes the handful of entities that show
     * up in practice.
     *
     * @return null if nothing but markup/whitespace remains
     */
    static String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        String text = html.replaceAll("<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim();
        return text.isBlank() ? null : text;
    }

    private SimpleClientHttpRequestFactory timeoutLimitedRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) TIMEOUT.toMillis());
        factory.setReadTimeout((int) TIMEOUT.toMillis());
        return factory;
    }
}
