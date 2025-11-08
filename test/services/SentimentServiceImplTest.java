package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Author: Java Developers
 */
public class SentimentServiceImplTest {

    private WSClient wsClient;
    private WSRequest wsRequest;
    private WSResponse wsResponse;
    private Config config;
    private SentimentServiceImpl service;

    @BeforeEach
    void setup() {
        wsClient = mock(WSClient.class);
        wsRequest = mock(WSRequest.class);
        wsResponse = mock(WSResponse.class);
        config = mock(Config.class);

        when(wsClient.url(anyString())).thenReturn(wsRequest);
        when(wsRequest.addQueryParameter(anyString(), anyString())).thenReturn(wsRequest);

        service = new SentimentServiceImpl(wsClient, config);
    }

    @Test
    void testAggregate_HappyMajority() {
        String result = service.aggregateSentiment(List.of("joy happy love amazing great win"));
        assertEquals(":-)", result);
    }

    @Test
    void testAggregate_SadMajority() {
        String result = service.aggregateSentiment(List.of("sad terrible awful cry pain tragedy"));
        assertEquals(":-(", result);
    }

    @Test
    void testAggregate_NeutralBalanced() {
        String result = service.aggregateSentiment(List.of("happy sad happy sad"));
        assertEquals(":-|", result);
    }

    @Test
    void testAggregate_NoKeywords() {
        String result = service.aggregateSentiment(List.of("neutral topic nothing special"));
        assertEquals(":-|", result);
    }

    @Test
    void testAggregate_EmptyList() {
        String result = service.aggregateSentiment(List.of());
        assertEquals(":-|", result);
    }

    @Test
    void testExtractDescriptions_ValidArray() throws Exception {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        var arr = root.putArray("articles");
        arr.addObject().put("description", "Happy day with joy and love");
        arr.addObject().put("description", "Sad and awful tragedy");

        var method = SentimentServiceImpl.class.getDeclaredMethod("extractDescriptions", JsonNode.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(service, root);

        assertEquals(2, result.size());
        assertTrue(result.get(0).contains("happy"));
    }

    @Test
    void testExtractDescriptions_NotArray() throws Exception {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("articles", "invalid");

        var method = SentimentServiceImpl.class.getDeclaredMethod("extractDescriptions", JsonNode.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(service, root);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractDescriptions_EmptyArray() throws Exception {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.putArray("articles"); // empty

        var method = SentimentServiceImpl.class.getDeclaredMethod("extractDescriptions", JsonNode.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(service, root);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSentimentForQueryAsync() {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        var arr = json.putArray("articles");
        arr.addObject().put("description", "happy amazing great love");

        when(wsResponse.asJson()).thenReturn(json);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));

        String emoji = service.sentimentForQuery("joy").toCompletableFuture().join();

        assertEquals(":-)", emoji);
    }

    @Test
    void testSentimentForQueryEmptyArticles() {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.putArray("articles"); // empty
        when(wsResponse.asJson()).thenReturn(json);
        when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));

        String emoji = service.sentimentForQuery("none").toCompletableFuture().join();
        assertEquals(":-|", emoji);
    }
}
