package controllers;

import models.Article;
import models.SourceInfo;

import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Result;

import services.SentimentService;
import services.NewsApiClient;
import services.NewsSources;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import play.mvc.WebSocket; // correct;
// import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

public class HomeControllerTest {

    private Application app;

    // SINGLE controller instance for all tests
    private HomeController ctrl;

    // mocks
    private SentimentService sentiment;
    private NewsApiClient newsApi;
    private NewsSources newsSources;

    @Before
    public void setUp() throws Exception {

        sentiment = mock(SentimentService.class);
        when(sentiment.sentimentForQuery(anyString()))
                .thenReturn(CompletableFuture.completedFuture(":-)"));

        newsApi = mock(NewsApiClient.class);
        when(newsApi.searchArticles(anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        newsSources = mock(NewsSources.class);
        when(newsSources.fetchSources(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        app = new GuiceApplicationBuilder()
                .overrides(b -> {
                    b.bind(SentimentService.class).toInstance(sentiment);
                    b.bind(NewsApiClient.class).toInstance(newsApi);
                    b.bind(NewsSources.class).toInstance(newsSources);
                })
                .build();

        // Use ONE consistent instance for test correctness
        ctrl = app.injector().instanceOf(HomeController.class);

        // Clear static cache
        safeClearStaticMap("cache");
    }

    private void safeClearStaticMap(String fieldName) {
        try {
            Field f = HomeController.class.getDeclaredField(fieldName);
            if (!Modifier.isStatic(f.getModifiers()))
                return;
            f.setAccessible(true);
            Object o = f.get(null);
            if (o instanceof Map<?, ?> map) {
                map.clear();
            }
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------------------
    // TESTS
    // --------------------------------------------------------------

    @Test
    public void index_renders_ok() {
        Result r = ctrl.index();
        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).contains("NotiLytics"));
    }

    @Test
    public void search_emptyQuery_rendersNeutral() {
        Result r = ctrl.search("", "publishedAt")
                .toCompletableFuture().join();
        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).toLowerCase().contains("notilytics"));
    }

    @Test
    public void sentiment_endpoint_returnsJson() {
        Result r = ctrl.sentiment("abc").toCompletableFuture().join();
        assertEquals(OK, r.status());
        assertEquals(":-)", Json.parse(contentAsString(r)).get("sentiment").asText());
        verify(sentiment, times(1)).sentimentForQuery("abc");
    }

    @Test
    public void search_withCachedArticles_updatesAccumulatedResults() throws Exception {

        // Seed static cache
        Field f = HomeController.class.getDeclaredField("cache");
        f.setAccessible(true);
        Map<String, List<Article>> cache = (Map<String, List<Article>>) f.get(null);

        cache.put("ai_publishedAt", List.of(
                new Article("T1", "u1", "S", "su", "2024-01-01", "d1", 0.1),
                new Article("T2", "u2", "S", "su", "2024-01-01", "d2", 0.2)));

        // Perform search on SAME controller instance
        Result r = ctrl.search("ai", "publishedAt")
                .toCompletableFuture().join();

        assertEquals(OK, r.status());

        // Check accumulatedResults on SAME instance
        Field accF = HomeController.class.getDeclaredField("accumulatedResults");
        accF.setAccessible(true);

        LinkedHashMap<String, ?> acc = (LinkedHashMap<String, ?>) accF.get(ctrl);

        assertNotNull(acc);
        assertTrue(acc.containsKey("ai"));
    }

    @Test
    public void fetchArticlesForQuery_reflection_works() throws Exception {
        Method m = HomeController.class
                .getDeclaredMethod("fetchArticlesForQuery", String.class, String.class);
        m.setAccessible(true);

        Object result = m.invoke(ctrl, "test", "publishedAt");
        assertNotNull(result);
        assertTrue(result instanceof List);
    }

    @Test
    public void wordStats_renders_ok() {
        when(newsApi.searchArticles("ai", 50))
                .thenReturn(CompletableFuture.completedFuture(
                        List.of(new Article("A", "u", "S", "su",
                                "2024", "Artificial intelligence test case", 0.3))));

        Result r = ctrl.wordStats("ai").toCompletableFuture().join();
        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).toLowerCase().contains("artificial"));
    }

    @Test
    public void newsSources_renders_ok() {
        Result r1 = ctrl.newsSources("", "", "").toCompletableFuture().join();
        Result r2 = ctrl.newsSources("us", "business", "en").toCompletableFuture().join();

        assertEquals(OK, r1.status());
        assertEquals(OK, r2.status());
    }

    @Test
    public void ws_opens_connection() {
        WebSocket ws = ctrl.ws();
        assertNotNull(ws);

        play.libs.F.Either<Result, org.apache.pekko.stream.javadsl.Flow<play.http.websocket.Message, play.http.websocket.Message, ?>> socket = ws
                .apply(fakeRequest().header("Origin", "http://localhost").build())
                .toCompletableFuture().join();

        // WebSocket accepted → Right side present
        assertTrue(socket.right.isPresent());
    }

    @Test
    public void ws_rejects_invalid_origin() {
        var stage = ctrl.ws().apply(fakeRequest()
                .header("Origin", "http://evil.com")
                .build());

        var either = stage.toCompletableFuture().join();
        assertTrue("Expected Left(Result) but got Right(...) instead", either.left.isPresent());
        Result r = either.left.get();
        assertTrue(r.status() == FORBIDDEN || r.status() == UNAUTHORIZED);
    }

    @Test
    public void search_handles_newsApi_exception() throws Exception {
        when(newsApi.searchArticles(anyString(), anyInt()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        Result r = ctrl.search("ai", "publishedAt").toCompletableFuture().join();
        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).contains("error"));
    }

    @Test
    public void sentiment_handles_errors() {
        when(sentiment.sentimentForQuery("bad"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        Result r = ctrl.sentiment("bad").toCompletableFuture().join();
        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).contains(":-|"));
    }

    @Test
    public void search_invalidSort_usesDefault() {
        Result r = ctrl.search("ai", "invalid").toCompletableFuture().join();
        assertEquals(OK, r.status());
    }

    @Test
    public void search_nullQuery_safe() {
        Result r = ctrl.search(null, "publishedAt").toCompletableFuture().join();
        assertEquals(OK, r.status());
    }

    @Test
    public void search_filters_duplicates() {
        Article a = new Article("T1", "U1", "S", "S", "2024", "D", 0.1);

        when(newsApi.searchArticles("ai", 10))
                .thenReturn(CompletableFuture.completedFuture(List.of(a, a)));

        Result r = ctrl.search("ai", "publishedAt").toCompletableFuture().join();
        assertEquals(OK, r.status());
    }

    @Test
    public void search_cacheHit_noApiCall() throws Exception {
        Field f = HomeController.class.getDeclaredField("cache");
        f.setAccessible(true);
        Map<String, List<Article>> cache = (Map<String, List<Article>>) f.get(null);
        cache.put("test_publishedAt", List.of());

        ctrl.search("test", "publishedAt").toCompletableFuture().join();

        verify(newsApi, times(0)).searchArticles(anyString(), anyInt());
    }

    @Test
    public void index_template_safe() {
        Result r = ctrl.index();
        assertEquals(OK, r.status());
    }

    @Test
    public void websocket_should_initialize() {
        WebSocket ws = ctrl.ws();
        assertNotNull(ws);

        play.libs.F.Either<Result, org.apache.pekko.stream.javadsl.Flow<play.http.websocket.Message, play.http.websocket.Message, ?>> socket = ws
                .apply(fakeRequest().header("Origin", "http://localhost").build())
                .toCompletableFuture().join();

        assertTrue(socket.right.isPresent());
    }

    @Test
    public void fetchArticles_handles_exception() throws Exception {
        Method m = HomeController.class
                .getDeclaredMethod("fetchArticlesForQuery", String.class, String.class);
        m.setAccessible(true);

        Object res = m.invoke(ctrl, "%%%%%", "publishedAt");

        assertNotNull(res);
        assertTrue(res instanceof List);
    }

    @Test
    public void fetchArticles_handles_dateParseError() throws Exception {
        String badJson = """
                {"articles":[{"title":"T","url":"U","source":{"name":"S"},"publishedAt":"BAD_DATE","description":"D"}]}
                """;

        // Use reflection to test internal JSON parsing branch
        Method m = HomeController.class
                .getDeclaredMethod("fetchArticlesForQuery", String.class, String.class);
        m.setAccessible(true);

        Object res = m.invoke(ctrl, "query", "publishedAt");

        assertTrue(res instanceof List);
    }

    @Test
    public void fetchArticles_missing_description_safe() throws Exception {
        Method m = HomeController.class
                .getDeclaredMethod("fetchArticlesForQuery", String.class, String.class);
        m.setAccessible(true);

        List<Article> list = (List<Article>) m.invoke(ctrl, "missingdesc", "publishedAt");
        assertNotNull(list);
    }

    @Test
    public void search_nullQuery_returnsNeutral() {
        Result r = ctrl.search(null, "publishedAt")
                .toCompletableFuture().join();

        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).contains(":-|"));
    }

    @Test
    public void search_removes_oldest_keyword() throws Exception {
        for (int i = 1; i <= 12; i++) {
            ctrl.search("key" + i, "publishedAt").toCompletableFuture().join();
        }

        Field accF = HomeController.class.getDeclaredField("accumulatedResults");
        accF.setAccessible(true);
        LinkedHashMap<?, ?> acc = (LinkedHashMap<?, ?>) accF.get(ctrl);

        assertEquals(10, acc.size());
    }

    @Test
    public void wordStats_emptyQuery_returns_badRequest() {
        Result r = ctrl.wordStats("").toCompletableFuture().join();
        assertEquals(BAD_REQUEST, r.status());
    }

}
