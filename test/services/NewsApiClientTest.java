package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import org.junit.Test;
import play.libs.Json;
import play.libs.ws.*;
import models.Article;
import models.SourceInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NewsApiClient}.
 *
 * Verifies correct behavior in both:
 *   - STUB MODE (when no NewsAPI key exists)
 *   - REAL MODE (when a key exists and WSClient is used)
 *
 * Uses Mockito to stub WSClient and simulate NewsAPI responses.
 *
 * @author Manush Shah
 */
public class NewsApiClientTest {

    // ------------------------------------------------------------
    // Helper: Build mocked WSResponse returning JSON + status code
    // ------------------------------------------------------------

    /**
     * Creates a mocked WSResponse with given JSON content and status.
     */
    private WSResponse mockJsonResponse(JsonNode node, int status) {
        WSResponse resp = mock(WSResponse.class);
        when(resp.asJson()).thenReturn(node);
        when(resp.getStatus()).thenReturn(status);
        return resp;
    }

    /**
     * Creates a mocked WSRequest whose .get() immediately returns
     * a completed future with the provided WSResponse.
     */
    private WSRequest mockRequestReturning(WSResponse resp) {
        WSRequest req = mock(WSRequest.class);

        when(req.get()).thenReturn(CompletableFuture.completedFuture(resp));
        when(req.addQueryParameter(any(), any())).thenReturn(req);
        when(req.addHeader(any(), any())).thenReturn(req);

        return req;
    }

    // ===================================================================
    //  STUB MODE TESTS (no API key → local mock data returned)
    // ===================================================================

    /**
     * Ensures sourceProfileByName() returns stubbed data when
     * no NewsAPI key is configured.
     */
    @Test
    public void stub_mode_sourceProfile_works() {
        WSClient ws = mock(WSClient.class);
        Config cfg = mock(Config.class);

        when(cfg.hasPath("newsapi.key")).thenReturn(false); // stub mode

        NewsApiClient client = new NewsApiClient(ws, cfg);

        SourceInfo out = client.sourceProfileByName("BBC")
                .toCompletableFuture().join();

        assertEquals("BBC", out.name());
        assertEquals("demo-id", out.id());
        assertNotNull(out.articles());
        assertTrue(out.articles().size() >= 1);
    }

    /**
     * Ensures searchArticles() returns stubbed article list
     * when no API key is present.
     */
    @Test
    public void stub_mode_searchArticles_works() {
        WSClient ws = mock(WSClient.class);
        Config cfg = mock(Config.class);

        when(cfg.hasPath("newsapi.key")).thenReturn(false);

        NewsApiClient client = new NewsApiClient(ws, cfg);

        List<Article> out = client.searchArticles("AI", 5)
                .toCompletableFuture().join();

        assertNotNull(out);
        assertTrue(out.size() >= 1);
        assertNotNull(out.get(0).getTitle());
    }

    // ===================================================================
    //  REAL MODE TESTS (API key present → WSClient should be used)
    // ===================================================================

    /**
     * Ensures sourceProfileByName() gracefully handles error JSON
     * or non-200 API responses in real mode.
     */
    @Test
    public void real_mode_sourceProfile_handles_failed_response() {
        WSClient ws = mock(WSClient.class);
        Config cfg = mock(Config.class);

        when(cfg.hasPath("newsapi.key")).thenReturn(true);
        when(cfg.getString("newsapi.key")).thenReturn("X"); // fake API key

        // Simulated "error" response JSON
        ObjectNode bad = Json.newObject();
        bad.put("status", "error");

        WSResponse resp = mockJsonResponse(bad, 400);
        WSRequest req = mockRequestReturning(resp);

        when(ws.url(anyString())).thenReturn(req);

        NewsApiClient client = new NewsApiClient(ws, cfg);

        SourceInfo s = client.sourceProfileByName("CNN")
                .toCompletableFuture().join();

        assertEquals("CNN", s.name());
        assertEquals("API key invalid or request failed.", s.description());
    }

    /**
     * Ensures searchArticles() correctly parses a valid JSON response
     * returned from NewsAPI in real mode.
     */
    @Test
    public void real_mode_searchArticles_handles_ok_response() {
        WSClient ws = mock(WSClient.class);
        Config cfg = mock(Config.class);

        when(cfg.hasPath("newsapi.key")).thenReturn(true);
        when(cfg.getString("newsapi.key")).thenReturn("X");

        // Construct fake but valid NewsAPI JSON
        ObjectNode root = Json.newObject();
        root.put("status", "ok");

        var arr = root.putArray("articles");

        ObjectNode a = Json.newObject();
        a.put("title", "Hello");
        a.put("url", "https://x.com");

        ObjectNode src = Json.newObject();
        src.put("name", "ABC");
        a.set("source", src);

        a.put("description", "test desc");
        a.put("publishedAt", LocalDateTime.now().toString() + "Z");

        arr.add(a);

        WSResponse resp = mockJsonResponse(root, 200);
        WSRequest req = mockRequestReturning(resp);

        when(ws.url(anyString())).thenReturn(req);

        NewsApiClient client = new NewsApiClient(ws, cfg);

        List<Article> list = client.searchArticles("AI", 5)
                .toCompletableFuture().join();

        assertEquals(1, list.size());
        assertEquals("Hello", list.get(0).getTitle());
    }
}
