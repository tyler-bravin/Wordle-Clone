package dev.tylerbravin.wordle.service;

import dev.tylerbravin.wordle.config.GameProperties;
import dev.tylerbravin.wordle.dto.WordDefinitionResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link DictionaryService#toResponse} and
 * {@link DictionaryService#toResponseFromWiktionary} directly against
 * hand-built fixtures, rather than the network-calling {@code fetch} methods -
 * no real HTTP call is made, so this doesn't depend on either external API
 * being up.
 */
class DictionaryServiceTest {

    private final DictionaryService service = new DictionaryService(
            new GameProperties(6, 5, LocalDate.of(2024, 1, 1), 20240101L, "https://example.invalid", "https://fallback.invalid",
                    Duration.ofDays(2), Duration.ofDays(30))
    );

    @Test
    void mapsFirstDefinitionAndPhoneticFromEntries() {
        var entry = new DictionaryApiEntry.Entry(
                "crane",
                "/kreɪn/",
                List.of(new DictionaryApiEntry.Meaning(
                        "noun",
                        List.of(new DictionaryApiEntry.Definition("A large wading bird.", "The crane stood in the marsh."))
                ))
        );

        WordDefinitionResponse response = service.toResponse("crane", List.of(entry));

        assertThat(response.found()).isTrue();
        assertThat(response.phonetic()).isEqualTo("/kreɪn/");
        assertThat(response.meanings()).hasSize(1);
        assertThat(response.meanings().get(0).partOfSpeech()).isEqualTo("noun");
        assertThat(response.meanings().get(0).definition()).isEqualTo("A large wading bird.");
        assertThat(response.meanings().get(0).example()).isEqualTo("The crane stood in the marsh.");
    }

    @Test
    void capsMeaningsAtThreeAcrossMultipleEntries() {
        var entry = new DictionaryApiEntry.Entry("crane", null, List.of(
                meaningWith("noun", "def 1"),
                meaningWith("verb", "def 2"),
                meaningWith("noun", "def 3"),
                meaningWith("noun", "def 4")
        ));

        WordDefinitionResponse response = service.toResponse("crane", List.of(entry));

        assertThat(response.meanings()).hasSize(3);
    }

    @Test
    void emptyEntryListMeansNotFound() {
        WordDefinitionResponse response = service.toResponse("zzzzz", List.of());
        assertThat(response.found()).isFalse();
        assertThat(response.meanings()).isEmpty();
    }

    @Test
    void entriesWithNoUsableDefinitionsMeanNotFound() {
        var entry = new DictionaryApiEntry.Entry("crane", "/kreɪn/", List.of(
                new DictionaryApiEntry.Meaning("noun", List.of())
        ));

        WordDefinitionResponse response = service.toResponse("crane", List.of(entry));

        assertThat(response.found()).isFalse();
    }

    @Test
    void missingPhoneticFallsBackToNull() {
        var entry = new DictionaryApiEntry.Entry("crane", null, List.of(meaningWith("noun", "def 1")));

        WordDefinitionResponse response = service.toResponse("crane", List.of(entry));

        assertThat(response.phonetic()).isNull();
    }

    private DictionaryApiEntry.Meaning meaningWith(String partOfSpeech, String definition) {
        return new DictionaryApiEntry.Meaning(partOfSpeech, List.of(new DictionaryApiEntry.Definition(definition, null)));
    }

    @Test
    void mapsWiktionaryDefinitionAndExampleStrippingMarkupAndLowercasingPartOfSpeech() {
        var entry = new WiktionaryEntry.PartOfSpeech("Determiner", List.of(
                new WiktionaryEntry.Definition(
                        "<span>All</span> of a <a href=\"/wiki/countable\">countable</a> group.",
                        List.of("<b>Every</b> person stood."))
        ));

        WordDefinitionResponse response = service.toResponseFromWiktionary("every", Map.of("en", List.of(entry)));

        assertThat(response.found()).isTrue();
        assertThat(response.phonetic()).isNull();
        assertThat(response.meanings()).hasSize(1);
        assertThat(response.meanings().get(0).partOfSpeech()).isEqualTo("determiner");
        assertThat(response.meanings().get(0).definition()).isEqualTo("All of a countable group.");
        assertThat(response.meanings().get(0).example()).isEqualTo("Every person stood.");
    }

    @Test
    void wiktionaryResponseWithNoEnglishEntriesMeansNotFound() {
        var frenchOnly = Map.of("fr", List.of(new WiktionaryEntry.PartOfSpeech(
                "Nom", List.of(new WiktionaryEntry.Definition("une grue", null)))));

        WordDefinitionResponse response = service.toResponseFromWiktionary("grue", frenchOnly);

        assertThat(response.found()).isFalse();
    }

    @Test
    void wiktionarySensesWithAnUnrenderedUsageQualifierAreSkipped() {
        // Regression test for a real find: Wiktionary's REST endpoint marks dated/
        // slang/vulgar senses with an empty "usage-label-sense" span rather than
        // the actual qualifier text, so "zesty" would otherwise surface "(of a
        // man) flamboyantly gay" as if it were a plain, unqualified sense.
        var entry = new WiktionaryEntry.PartOfSpeech("Adjective", List.of(
                new WiktionaryEntry.Definition("Having a piquant taste; spicy.", null),
                new WiktionaryEntry.Definition(
                        "<span class=\"usage-label-sense\" about=\"#mwt9\" typeof=\"mw:Transclusion\"></span> (of a man) Flamboyantly gay.",
                        null)
        ));

        WordDefinitionResponse response = service.toResponseFromWiktionary("zesty", Map.of("en", List.of(entry)));

        assertThat(response.meanings()).hasSize(1);
        assertThat(response.meanings().get(0).definition()).isEqualTo("Having a piquant taste; spicy.");
    }

    @Test
    void wiktionaryEntriesThatAreBlankAfterStrippingMarkupMeanNotFound() {
        var entry = new WiktionaryEntry.PartOfSpeech("Noun", List.of(
                new WiktionaryEntry.Definition("<span></span>", null)
        ));

        WordDefinitionResponse response = service.toResponseFromWiktionary("zzzzz", Map.of("en", List.of(entry)));

        assertThat(response.found()).isFalse();
    }

    @Test
    void stripHtmlRemovesTagsAndDecodesCommonEntities() {
        assertThat(DictionaryService.stripHtml("<a href=\"/wiki/all\">All</a> &amp; <b>every</b> one"))
                .isEqualTo("All & every one");
        assertThat(DictionaryService.stripHtml("<span></span>")).isNull();
        assertThat(DictionaryService.stripHtml(null)).isNull();
    }
}
