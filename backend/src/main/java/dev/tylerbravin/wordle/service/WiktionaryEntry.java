package dev.tylerbravin.wordle.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mirrors the response shape of {@code en.wiktionary.org/api/rest_v1/page/definition},
 * the fallback dictionary source, kept private to {@link DictionaryService} for the
 * same reason as {@link DictionaryApiEntry}. The top-level response is a map of
 * language code to entries (e.g. {@code {"en": [...], "fr": [...]}}); only the
 * {@code "en"} entry is used.
 * <p>
 * Definition and example text here contains embedded Wiktionary markup (links,
 * spans) rather than plain text like the primary source, so it needs stripping
 * before use - see {@link DictionaryService#stripHtml}.
 */
final class WiktionaryEntry {

    private WiktionaryEntry() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PartOfSpeech(String partOfSpeech, List<Definition> definitions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Definition(String definition, List<String> examples) {
    }
}
