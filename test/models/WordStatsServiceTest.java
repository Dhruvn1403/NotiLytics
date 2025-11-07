package models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import services.NewsApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WordStatsService.
 * Validates tokenization, counting, sorting, and null safety.
 *
 * Uses a mocked NewsApiClient to avoid real HTTP calls.
 *
 * Author: Varun Oza
 */
public class WordStatsServiceTest {

    private NewsApiClient mockClient;
    private WordStatsService service;

    @BeforeEach
    public void setup() {
        mockClient = Mockito.mock(NewsApiClient.class);
        service = new WordStatsService(mockClient);
    }

    @Test
    public void testComputeWordStatsCountsCorrectly() {
        List<Article> articles = List.of(
                new Article("AI beats humans", "url1", "Src1", "", "Auth", "AI is amazing and AI changes everything", 0.0),
                new Article("Machine Learning", "url2", "Src2", "", "Auth", "Machine learning and AI work together", 0.0),
                new Article("Data Science", "url3", "Src3", "", "Auth", "AI and data science revolution", 0.0)
        );

        when(mockClient.searchArticles(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(articles));

        CompletionStage<List<WordStatsService.WordCount>> future = service.computeWordStats("AI");
        List<WordStatsService.WordCount> stats = future.toCompletableFuture().join();

        assertFalse(stats.isEmpty(), "Word stats should not be empty");
        assertTrue(stats.stream().anyMatch(w -> w.getWord().equals("ai")), "Should contain 'ai'");
        assertTrue(stats.stream().anyMatch(w -> w.getWord().equals("and")), "Should contain 'and'");

        long firstCount = stats.get(0).getCount();
        long lastCount = stats.get(stats.size() - 1).getCount();
        assertTrue(firstCount >= lastCount, "Results should be sorted by frequency descending");

        verify(mockClient, times(1)).searchArticles(anyString(), anyInt());
    }

    @Test
    public void testComputeWordStatsEmptyResponse() {
        when(mockClient.searchArticles(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        CompletionStage<List<WordStatsService.WordCount>> future = service.computeWordStats("EmptyTopic");
        List<WordStatsService.WordCount> stats = future.toCompletableFuture().join();

        assertNotNull(stats, "Result should not be null");
        assertTrue(stats.isEmpty(), "If no articles returned, stats list should be empty");

        verify(mockClient, times(1)).searchArticles(anyString(), anyInt());
    }

    @Test
    public void testComputeWordStatsHandlesNullDescriptions() {
        List<Article> articles = List.of(
                new Article("A1", "url1", "Src1", "", "Auth", null, 0.0),
                new Article("A2", "url2", "Src2", "", "Auth", "AI is cool", 0.0)
        );

        when(mockClient.searchArticles(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(articles));

        CompletionStage<List<WordStatsService.WordCount>> future = service.computeWordStats("AI");
        List<WordStatsService.WordCount> stats = future.toCompletableFuture().join();

        assertNotNull(stats, "Result should not be null");
        assertFalse(stats.isEmpty(), "Should skip null descriptions but still process valid ones");

        verify(mockClient, times(1)).searchArticles(anyString(), anyInt());
    }
}
