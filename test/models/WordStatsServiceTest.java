package models;

import org.junit.Test;
import org.mockito.Mockito;
import services.NewsApiClient;
/**
 * @author Java Developers
 */
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class WordStatsServiceTest {

    @Test
    public void computeWordStats_counts_and_normalizes() {
        // Mock API
        NewsApiClient api = Mockito.mock(NewsApiClient.class);

        // Articles designed to exercise:
        // - URL stripping
        // - HTML tag stripping
        // - case-folding & diacritics (São -> sao)
        // - apostrophes: internal apostrophes are kept ("AI's" -> "ai's")
        List<Article> sample = List.of(
                new Article("t1", "u1", "s", "su", "p",
                        "AI's rise in São Paulo <b>rocks</b> http://x/x", 0.0),
                new Article("t2", "u2", "s", "su", "p",
                        "AI, AI... start-up’s AI!", 0.0),
                new Article("t3", "u3", "s", "su", "p",
                        null, 0.0) // null description should be ignored
        );

        when(api.searchArticles(Mockito.eq("ai"), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(sample));

        WordStatsService svc = new WordStatsService(api);

        var stats = svc.computeWordStats("ai").toCompletableFuture().join();
        assertNotNull(stats);
        assertFalse(stats.isEmpty());

        // "AI, AI... AI!" -> 3 occurrences of "ai"
        long aiCount = stats.stream()
                .filter(w -> w.getWord().equals("ai"))
                .mapToLong(WordStatsService.WordCount::getCount)
                .findFirst().orElse(0);
        assertEquals(3, aiCount);

        // "AI's" -> "ai's" (internal apostrophe preserved)
        long aisApostropheCount = stats.stream()
                .filter(w -> w.getWord().equals("ai's"))
                .mapToLong(WordStatsService.WordCount::getCount)
                .findFirst().orElse(0);
        assertEquals(1, aisApostropheCount);

        // Diacritics removed: "São" -> "sao"
        boolean hasSao = stats.stream().anyMatch(w -> w.getWord().equals("sao"));
        assertTrue(hasSao);

        // URL removed — should not see "http"
        boolean hasHttp = stats.stream().anyMatch(w -> w.getWord().equals("http"));
        assertFalse(hasHttp);
    }

    @Test
    public void computeWordStats_handles_empty_from_api() {
        NewsApiClient api = Mockito.mock(NewsApiClient.class);
        when(api.searchArticles(Mockito.eq("none"), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        WordStatsService svc = new WordStatsService(api);
        var stats = svc.computeWordStats("none").toCompletableFuture().join();
        assertNotNull(stats);
        assertTrue(stats.isEmpty());
    }
}
