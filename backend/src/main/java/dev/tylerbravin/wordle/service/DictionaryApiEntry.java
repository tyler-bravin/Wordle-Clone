package dev.tylerbravin.wordle.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mirrors the response shape of {@code api.dictionaryapi.dev}, kept private to
 * {@link DictionaryService} so the rest of the app never depends on a third
 * party's JSON structure - only {@link dev.tylerbravin.wordle.dto.WordDefinitionResponse}
 * crosses that boundary. {@code @JsonIgnoreProperties(ignoreUnknown = true)}
 * throughout so upstream schema additions (synonyms, license info, audio
 * URLs, etc.) don't break deserialization.
 */
final class DictionaryApiEntry {

    private DictionaryApiEntry() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(String word, String phonetic, List<Meaning> meanings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Meaning(String partOfSpeech, List<Definition> definitions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Definition(String definition, String example) {
    }
}
