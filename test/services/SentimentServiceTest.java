package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import play.libs.ws.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SentimentServiceTest {

    /** Build a mocked WS pipeline for sentimentForQuery() */
    private WSClient mockWs(JsonNode json) {
        WSClient ws = mock(WSClient.class);
        WSRequest req = mock(WSRequest.class);
        WSResponse resp = mock(WSResponse.class);

        // WSClient.url() → WSRequest
        when(ws.url(anyString())).thenReturn(req);

        // req.addQueryParameter() → return same request
        when(req.addQueryParameter(anyString(), anyString())).thenReturn(req);

        // req.get() returns WSResponse future
        CompletionStage<WSResponse> stage = CompletableFuture.completedFuture(resp);
        when(req.get()).thenReturn(stage);

        // Response JSON
        when(resp.asJson()).thenReturn(json);

        return ws;
    }

    /** Helper to easily create fake JSON */
    private JsonNode jsonArticles(String... descriptions) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ArrayNode arr = root.putArray("articles");

        for (String d : descriptions) {
            ObjectNode art = JsonNodeFactory.instance.objectNode();
            art.put("description", d);
            arr.add(art);
        }
        return root;
    }

    // ---------------------------------------------------------
    // TEST 1 — Empty → neutral ":-|"
    // ---------------------------------------------------------
    @Test
    public void sentiment_empty_returnsNeutral() {
        WSClient ws = mockWs(jsonArticles());
        SentimentService svc = new SentimentService(ws, null);

        String out = svc.sentimentForQuery("x").toCompletableFuture().join();
        assertEquals(":-|", out);
    }

    // ---------------------------------------------------------
    // TEST 2 — Happy keywords → ":-)"
    // ---------------------------------------------------------
    @Test
    public void sentiment_happy_detected() {
        JsonNode json = jsonArticles("amazing fantastic great love wonderful victory");

        WSClient ws = mockWs(json);
        SentimentService svc = new SentimentService(ws, null);

        String out = svc.sentimentForQuery("happy").toCompletableFuture().join();
        assertEquals(":-)", out);
    }

    // ---------------------------------------------------------
    // TEST 3 — Sad keywords → ":-("
    // ---------------------------------------------------------
    @Test
    public void sentiment_sad_detected() {
        JsonNode json = jsonArticles("terrible awful horrible hate cry disaster");

        WSClient ws = mockWs(json);
        SentimentService svc = new SentimentService(ws, null);

        String out = svc.sentimentForQuery("sad").toCompletableFuture().join();
        assertEquals(":-(", out);
    }

    // ---------------------------------------------------------
    // TEST 4 — Mixed → neutral
    // ---------------------------------------------------------
    @Test
    public void sentiment_mixed_returnsNeutral() {
        JsonNode json = jsonArticles("good", "bad");

        WSClient ws = mockWs(json);
        SentimentService svc = new SentimentService(ws, null);

        String out = svc.sentimentForQuery("mixed").toCompletableFuture().join();
        assertEquals(":-|", out);
    }
}
