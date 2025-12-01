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

/**
 * Unit tests for {@link SentimentService}.
 *
 * <p>
 * These tests verify the correctness of sentiment classification logic using mocked
 * {@link WSClient}, {@link WSRequest}, and {@link WSResponse} objects.
 * No real HTTP calls are made — instead, JSON responses are simulated to test
 * SentimentService behavior under different sentiment conditions.
 * </p>
 *
 * <p>The sentiment returned by the service uses these symbols:
 * <ul>
 *   <li>":-)" — positive sentiment</li>
 *   <li>":-(" — negative sentiment</li>
 *   <li>":-|" — neutral or unclear sentiment</li>
 * </ul>
 * </p>
 *
 * <p>Each test builds a JSON structure in the format:</p>
 * <pre>
 * {
 *   "articles": [
 *     { "description": "text ..." },
 *     ...
 *   ]
 * }
 * </pre>
 */
public class SentimentServiceTest {

    /**
     * Creates a fully mocked WSClient pipeline that simulates a GET request returning
     * the provided JSON.
     *
     * <p>This mock setup ensures:
     * <ul>
     *   <li>{@code ws.url(...)} returns a mock {@code WSRequest}</li>
     *   <li>{@code req.addQueryParameter(...)} returns same request instance</li>
     *   <li>{@code req.get()} returns a completed future wrapping a mock {@code WSResponse}</li>
     *   <li>{@code resp.asJson()} returns the provided JSON</li>
     * </ul>
     * </p>
     *
     * @param json The JSON that should be returned when the request is executed
     * @return A mocked WSClient configured to return the given JSON
     */
    private WSClient mockWs(JsonNode json) {
        WSClient ws = mock(WSClient.class);
        WSRequest req = mock(WSRequest.class);
        WSResponse resp = mock(WSResponse.class);

        when(ws.url(anyString())).thenReturn(req);
        when(req.addQueryParameter(anyString(), anyString())).thenReturn(req);

        CompletionStage<WSResponse> future = CompletableFuture.completedFuture(resp);
        when(req.get()).thenReturn(future);
        when(resp.asJson()).thenReturn(json);

        return ws;
    }

    /**
     * Utility method for constructing JSON representing multiple news articles.
     *
     * <p>Resulting JSON is shaped as:</p>
     * <pre>
     * {
     *   "articles": [
     *     { "description": "desc1" },
     *     { "description": "desc2" }
     *   ]
     * }
     * </pre>
     *
     * @param descriptions Zero or more article description strings
     * @return JSON node with an "articles" array containing the descriptions
     */
    private JsonNode jsonArticles(String... descriptions) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ArrayNode arr = root.putArray("articles");

        for (String d : descriptions) {
            ObjectNode article = JsonNodeFactory.instance.objectNode();
            article.put("description", d);
            arr.add(article);
        }

        return root;
    }

    /**
     * Tests that when no articles are returned from the API,
     * the sentiment is evaluated as neutral (":-|").
     */
    @Test
    public void sentiment_empty_returnsNeutral() {
        WSClient ws = mockWs(jsonArticles());
        SentimentService svc = new SentimentService(ws, null);

        String result = svc.sentimentForQuery("anything").toCompletableFuture().join();
        assertEquals(":-|", result);
    }

    /**
     * Tests that descriptions containing clearly positive words
     * (e.g., amazing, fantastic) are correctly detected as positive,
     * returning the happy emoticon ":-)".
     */
    @Test
    public void sentiment_happy_detected() {
        JsonNode json = jsonArticles("amazing fantastic great love wonderful victory");

        WSClient ws = mockWs(json);
        SentimentService svc = new SentimentService(ws, null);

        String result = svc.sentimentForQuery("happy").toCompletableFuture().join();
        assertEquals(":-)", result);
    }

    /**
     * Tests that descriptions containing negative keywords
     * (e.g., terrible, awful, disaster) are correctly identified
     * as negative sentiment and return ":-(".
     */
    @Test
    public void sentiment_sad_detected() {
        JsonNode json = jsonArticles("terrible awful horrible hate cry disaster");

        WSClient ws = mockWs(json);
        SentimentService svc = new SentimentService(ws, null);

        String result = svc.sentimentForQuery("sad").toCompletableFuture().join();
        assertEquals(":-(", result);
    }

    /**
     * Tests that when positive and negative keywords both appear
     * in the article descriptions, the sentiment score is neutral
     * and returns ":-|".
     */
    @Test
    public void sentiment_mixed_returnsNeutral() {
        JsonNode json = jsonArticles("good", "bad");

        WSClient ws = mockWs(json);
        SentimentService svc = new SentimentService(ws, null);

        String result = svc.sentimentForQuery("mixed").toCompletableFuture().join();
        assertEquals(":-|", result);
    }
}
