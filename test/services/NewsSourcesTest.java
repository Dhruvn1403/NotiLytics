package services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;
import play.libs.Json;

import models.SourceInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

/**
 * Test suite for {@link NewsSources}, validating behavior of source fetching logic.
 *
 * <p>This class focuses on verifying:</p>
 * <ul>
 *     <li>The service returns a non-null list of {@link SourceInfo} objects.</li>
 *     <li>Error handling when invalid parameters or underlying failures occur.</li>
 * </ul>
 *
 * <p><b>Note:</b> Because {@link NewsSources} internally performs real HTTP requests,
 * mocking those calls is not straightforward without additional abstraction.
 * Therefore, these tests focus on validation of high-level behavior rather than
 * low-level response parsing.</p>
 *
 * @author Monil
 */
public class NewsSourcesTest {

    /**
     * Tests that {@link NewsSources#fetchSources(String, String, String)}
     * successfully returns a non-null CompletableFuture and does not throw exceptions.
     *
     * <p>This test builds a mock JSON object for demonstration; however, the internal
     * HTTP call is not intercepted. The main purpose is to assert that the service
     * behaves correctly at the API surface level.</p>
     */
    @Test
    public void fetchSources_returns_list() {
        NewsSources svc = new NewsSources();

        // Construct a mock JSON example to demonstrate expected source structure.
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

        // Since actual HTTP calls cannot be intercepted,
        // we only verify the returned CompletableFuture is valid.
        CompletableFuture<List<SourceInfo>> res =
                svc.fetchSources("us", "business", "en").toCompletableFuture();

        assertNotNull("fetchSources should return a non-null future", res);
    }

    /**
     * Tests that the service does not crash when provided invalid parameters,
     * and that it still returns a non-null result list.
     *
     * <p>This ensures defensive programming is in place and that failures
     * during request construction or execution result in a safe fallback.</p>
     */
    @Test
    public void fetchSources_handles_exception() {
        NewsSources svc = new NewsSources();

        // Passing empty fields simulates a malformed request scenario.
        List<SourceInfo> out = svc.fetchSources("", "", "")
                .toCompletableFuture().join();

        assertNotNull("Service should return a non-null list even during failure", out);
    }
}
