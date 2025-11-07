package controllers;

import models.WordStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.mvc.Result;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

/**
 * Unit test for WordStatsController.
 * Verifies correct rendering and integration with mock WordStatsService.
 *
 * Author: Varun Oza
 */
public class WordStatsControllerTest {

    private WordStatsService mockService;
    private WordStatsController controller;

    @BeforeEach
    public void setup() {
        mockService = mock(WordStatsService.class);
        controller = new WordStatsController(mockService);
    }

    @Test
    public void testShowWordStatsRendersPage() {
        List<WordStatsService.WordCount> dummyStats = List.of(
                new WordStatsService.WordCount("ai", 5),
                new WordStatsService.WordCount("data", 3)
        );
        when(mockService.computeWordStats(anyString()))
                .thenReturn(CompletableFuture.completedFuture(dummyStats));

        Result result = controller.showWordStats(fakeRequest().build(), "AI").toCompletableFuture().join();

        assertEquals(OK, result.status());
        String content = contentAsString(result);
        assertTrue(content.contains("Word statistics for: AI"));
        assertTrue(content.contains("ai"));
        assertTrue(content.contains("data"));
    }
}
