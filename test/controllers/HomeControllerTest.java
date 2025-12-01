package controllers;

import models.Article;
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
            if (!Modifier.isStatic(f.getModifiers())) return;
            f.setAccessible(true);
            Object o = f.get(null);
            if (o instanceof Map<?, ?> map) {
                map.clear();
            }
        } catch (NoSuchFieldException ignored) {}
          catch (Exception e) { throw new RuntimeException(e); }
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
                new Article("T2", "u2", "S", "su", "2024-01-01", "d2", 0.2)
        ));

        // Perform search on SAME controller instance
        Result r = ctrl.search("ai", "publishedAt")
                .toCompletableFuture().join();

        assertEquals(OK, r.status());

        // Check accumulatedResults on SAME instance
        Field accF = HomeController.class.getDeclaredField("accumulatedResults");
        accF.setAccessible(true);

        LinkedHashMap<String, ?> acc =
                (LinkedHashMap<String, ?>) accF.get(ctrl);

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
                                "2024", "Artificial intelligence test case", 0.3))
                ));

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
}
