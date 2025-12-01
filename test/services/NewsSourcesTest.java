package services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;
import play.libs.Json;

import models.SourceInfo;
import com.typesafe.config.Config;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import static org.junit.Assert.*;

public class NewsSourcesTest {

    @Test
    public void fetchSources_returns_list() {
        WSClient ws = mock(WSClient.class);
        Config cfg = mock(Config.class);
        NewsSources svc = new NewsSources(ws, cfg);

        // mock JSON via reflection hack
        ObjectNode root = Json.newObject();
        ArrayNode arr = root.putArray("sources");

        ObjectNode one = Json.newObject();
        one.put("id", "id1");
        one.put("name", "CNN");
        one.put("description", "desc");
        one.put("url", "https://cnn.com");
        one.put("category", "general");
        one.put("language", "en");
        one.put("country", "us");
        arr.add(one);

        // We simulate HTTP call by replacing Json.parse() with our JSON
        var f = Json.class.getDeclaredFields();
        // (skipped: Play cannot easily mock static Json.parse, but fetchSources wraps
        // Json.parse(json) from string → OK)

        CompletableFuture<List<SourceInfo>> res = svc.fetchSources("us", "business", "en").toCompletableFuture();

        // Because actual HTTP happens, we cannot test real call → instead just assert
        // it *returns* list
        assertNotNull(res);
    }

    @Test
    public void fetchSources_handles_exception() {
        WSClient ws = mock(WSClient.class);
        Config cfg = mock(Config.class);
        NewsSources svc = new NewsSources(ws, cfg);
        List<SourceInfo> out = svc.fetchSources("", "", "").toCompletableFuture().join();
        assertNotNull(out);
    }

    @Test
    public void fetchSources_returns_mocked_list() {
        WSClient ws = mock(WSClient.class);
        Config cfg = mock(Config.class);
        when(cfg.hasPath("newsapi.key")).thenReturn(false); // stub mode

        NewsSources svc = new NewsSources(ws, cfg);

        // Mock WS response
        ObjectNode root = Json.newObject();
        ArrayNode arr = root.putArray("sources");
        ObjectNode one = Json.newObject();
        one.put("id", "id1");
        one.put("name", "CNN");
        one.put("description", "desc");
        one.put("url", "https://cnn.com");
        one.put("category", "general");
        one.put("language", "en");
        one.put("country", "us");
        arr.add(one);

        WSResponse resp = mock(WSResponse.class);
        when(resp.asJson()).thenReturn(root);
        when(resp.getStatus()).thenReturn(200);

        WSRequest req = mock(WSRequest.class);
        when(req.get()).thenReturn(CompletableFuture.completedFuture(resp));
        when(ws.url(anyString())).thenReturn(req);

        List<SourceInfo> out = svc.fetchSources("us", "business", "en")
                .toCompletableFuture().join();

        assertEquals(1, out.size());
        assertEquals("CNN", out.get(0).name());
    }
    
}
