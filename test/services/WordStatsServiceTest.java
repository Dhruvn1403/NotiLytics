package services;

import models.Article;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WordStatsServiceTest {

    @Test
    public void empty_articles_returns_empty() {
        NewsApiClient api = mock(NewsApiClient.class);
        when(api.searchArticles(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        WordStatsService svc = new WordStatsService(api);
        List<WordStatsService.WordCount> out =
                svc.computeWordStats("x").toCompletableFuture().join();

        assertTrue(out.isEmpty());
    }

    @Test
    public void computes_word_frequencies() {
        NewsApiClient api = mock(NewsApiClient.class);

        Article a = new Article("T","u","S","su","2024",
                "Artificial intelligence intelligence boom!",0.0);

        when(api.searchArticles(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of(a)));

        WordStatsService svc = new WordStatsService(api);
        List<WordStatsService.WordCount> out =
                svc.computeWordStats("x").toCompletableFuture().join();

        assertFalse(out.isEmpty());
        assertEquals("intelligence", out.get(0).getWord());
    }

    @Test
    public void wordCount_getters_work() {
        WordStatsService.WordCount wc =
                new WordStatsService.WordCount("hello", 5);

        assertEquals("hello", wc.getWord());
        assertEquals(5, wc.getCount());
}

}
 