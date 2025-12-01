package services;

import models.Article;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WordStatsService}.
 *
 * <p>
 * This test suite validates the behavior of the word-frequency analysis logic,
 * including:
 * </p>
 *
 * <ul>
 *   <li>Correct handling of empty API results</li>
 *   <li>Accurate word-count generation from article descriptions</li>
 *   <li>Correct behavior of the nested {@code WordCount} value class</li>
 * </ul>
 *
 * <p>
 * A mocked {@link NewsApiClient} is used so that no real HTTP calls occur.
 * </p>
 *
 * <p><b>Author:</b> Manush Shah</p>
 */
public class WordStatsServiceTest {

    /**
     * Verifies that when the News API returns no articles,
     * the word-statistics result is an empty list.
     *
     * <p>
     * Ensures that the system gracefully handles a valid but empty response,
     * without throwing exceptions.
     * </p>
     */
    @Test
    public void empty_articles_returns_empty() {
        NewsApiClient api = mock(NewsApiClient.class);

        // Mock API to return an empty article list
        when(api.searchArticles(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        WordStatsService svc = new WordStatsService(api);

        List<WordStatsService.WordCount> out =
                svc.computeWordStats("x").toCompletableFuture().join();

        assertTrue(out.isEmpty());
    }

    /**
     * Tests that {@link WordStatsService} correctly computes word frequencies
     * from article description text.
     *
     * <p>
     * The sample description intentionally repeats the word
     * <code>"intelligence"</code> to verify correct counting and ranking.
     * </p>
     */
    @Test
    public void computes_word_frequencies() {
        NewsApiClient api = mock(NewsApiClient.class);

        // Description containing repeated words
        Article a = new Article(
                "T", "u", "S", "su", "2024",
                "Artificial intelligence intelligence boom!",
                0.0
        );

        // Mock API to return our single test article
        when(api.searchArticles(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of(a)));

        WordStatsService svc = new WordStatsService(api);

        List<WordStatsService.WordCount> out =
                svc.computeWordStats("x").toCompletableFuture().join();

        assertFalse(out.isEmpty());
        assertEquals("intelligence", out.get(0).getWord());
    }

    /**
     * Tests that the {@link WordStatsService.WordCount} nested class
     * correctly stores and exposes its fields via getters.
     *
     * <p>
     * This is a simple sanity check ensuring the DTO behaves as expected.
     * </p>
     */
    @Test
    public void wordCount_getters_work() {
        WordStatsService.WordCount wc =
                new WordStatsService.WordCount("hello", 5);

        assertEquals("hello", wc.getWord());
        assertEquals(5, wc.getCount());
    }
}
