package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import models.Article;
import models.SourceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * author: Java Developers
 */
public class NewsApiClientImplTest {

    private WSClient wsClient;
    private WSRequest wsRequest;
    private WSResponse wsResponse;
    private Config config;
    private NewsApiClientImpl client;

    @BeforeEach
    void setup() {
        wsClient = mock(WSClient.class);
        wsRequest = mock(WSRequest.class);
        wsResponse = mock(WSResponse.class);
        config = mock(Config.class);

        when(wsClient.url(anyString())).thenReturn(wsRequest);
        when(wsRequest.addHeader(anyString(), anyString())).thenReturn(wsRequest);
        when(wsRequest.addQueryParameter(anyString(), anyString())).thenReturn(wsRequest);

        when(config.hasPath(anyString())).thenReturn(true);
        when(config.getString(anyString())).thenReturn("fake-key");

        client = new NewsApiClientImpl(wsClient, config);
    }

    @Test
    void testSearchArticles_Valid() {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put("status", "ok");
        var arr = json.putArray("articles");
        arr.addObject()
                .put("title", "AI Impact")
                .put("url", "https://example.com")
                .putObject("source").put("name", "TechSource")
                .put("description", "AI testing")
                .put("publishedAt", LocalDateTime.now().toString());

        when(wsResponse.getStatus()).thenReturn(200);
        when(wsResponse.asJson()).thenReturn(json);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));

        List<Article> result = client.searchArticles("AI", 5).toCompletableFuture().join();
        assertEquals(1, result.size());
        assertTrue(result.get(0).getTitle().contains("AI"));
    }

    @Test
    void testSearchArticles_NonJson() {
        when(wsResponse.asJson()).thenThrow(new RuntimeException("bad json"));
        when(wsResponse.getStatusText()).thenReturn("No JSON");
        when(wsResponse.getStatus()).thenReturn(400);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));

        List<Article> list = client.searchArticles("AI", 2).toCompletableFuture().join();
        assertTrue(list.isEmpty());
    }

    @Test
    void testHandleBranches() {
        ObjectNode ok = JsonNodeFactory.instance.objectNode();
        ok.put("status", "ok");
        when(wsResponse.getStatus()).thenReturn(200);
        when(wsResponse.asJson()).thenReturn(ok);
        assertNotNull(invokeHandle(client, wsResponse, "ok"));

        ObjectNode err = JsonNodeFactory.instance.objectNode();
        err.put("status", "error");
        err.put("code", "429");
        err.put("message", "rate limit");
        when(wsResponse.getStatus()).thenReturn(429);
        when(wsResponse.asJson()).thenReturn(err);
        assertNull(invokeHandle(client, wsResponse, "err"));
    }

    @Test
    void testSafeJson() {
        ObjectNode ok = JsonNodeFactory.instance.objectNode();
        when(wsResponse.asJson()).thenReturn(ok);
        assertNotNull(invokeSafeJson(client, wsResponse));

        when(wsResponse.asJson()).thenThrow(new RuntimeException("non-json"));
        when(wsResponse.getStatusText()).thenReturn("Bad");
        assertNull(invokeSafeJson(client, wsResponse));
    }

    @Test
    void testSourceProfile_Found() {
        ObjectNode src = JsonNodeFactory.instance.objectNode();
        src.put("status", "ok");
        var arr = src.putArray("sources");
        arr.addObject()
                .put("id", "bbc")
                .put("name", "BBC")
                .put("description", "News site")
                .put("url", "https://bbc.com")
                .put("category", "general")
                .put("language", "en")
                .put("country", "uk");

        ObjectNode art = JsonNodeFactory.instance.objectNode();
        art.put("status", "ok");
        var aArr = art.putArray("articles");
        aArr.addObject()
                .put("title", "Breaking Story")
                .put("url", "https://bbc.com/1")
                .put("description", "Top story")
                .put("publishedAt", LocalDateTime.now().toString());

        when(wsResponse.getStatus()).thenReturn(200);
        when(wsResponse.asJson()).thenReturn(src).thenReturn(art);
        when(wsRequest.get()).thenReturn(
                CompletableFuture.completedFuture(wsResponse),
                CompletableFuture.completedFuture(wsResponse)
        );

        SourceInfo info = client.sourceProfileByName("BBC").toCompletableFuture().join();
        assertEquals("BBC", info.name());
        assertFalse(info.articles().isEmpty());
    }

    @Test
    void testSourceProfile_NotFound() {
        ObjectNode src = JsonNodeFactory.instance.objectNode();
        src.put("status", "ok");
        src.putArray("sources");
        when(wsResponse.getStatus()).thenReturn(200);
        when(wsResponse.asJson()).thenReturn(src);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));

        SourceInfo info = client.sourceProfileByName("CNN").toCompletableFuture().join();
        assertEquals("Not found", info.description());
    }

    @Test
    void testSourceProfile_InvalidKey() {
        when(wsResponse.getStatus()).thenReturn(403);
        when(wsResponse.asJson()).thenThrow(new RuntimeException("invalid"));
        when(wsResponse.getStatusText()).thenReturn("Forbidden");
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));

        SourceInfo info = client.sourceProfileByName("Fox").toCompletableFuture().join();
        assertTrue(info.description().contains("invalid"));
    }

    @Test
    void testStubCoverage() {
        NewsApiClientStub stub = new NewsApiClientStub();
        SourceInfo info = stub.sourceProfileByName("BBC").toCompletableFuture().join();
        assertTrue(info.description().contains("Demo"));
        assertEquals(1, info.articles().size());

        List<Article> articles = stub.searchArticles("AI", 5).toCompletableFuture().join();
        assertEquals(2, articles.size());
        assertTrue(articles.get(0).getTitle().contains("Mock"));
    }

    private JsonNode invokeSafeJson(NewsApiClientImpl impl, WSResponse resp) {
        try {
            var m = NewsApiClientImpl.class.getDeclaredMethod("safeJson", WSResponse.class);
            m.setAccessible(true);
            return (JsonNode) m.invoke(impl, resp);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode invokeHandle(NewsApiClientImpl impl, WSResponse resp, String tag) {
        try {
            var m = NewsApiClientImpl.class.getDeclaredMethod("handle", WSResponse.class, String.class);
            m.setAccessible(true);
            return (JsonNode) m.invoke(impl, resp, tag);
        } catch (Exception e) {
            return null;
        }
    }
}
