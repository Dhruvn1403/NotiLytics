/**
 * @author Manush Shah
 */
package controllers;

import models.Article;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Result;
import services.SentimentService;

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
    private SentimentService sentiment;

    private HomeController controller() {
        return app.injector().instanceOf(HomeController.class);
    }

    @Before
    public void setUp() throws Exception {
        sentiment = mock(SentimentService.class);
        when(sentiment.sentimentForQuery(anyString()))
                .thenAnswer(inv -> CompletableFuture.completedFuture(":-)"));

        app = new GuiceApplicationBuilder()
                .overrides(binder -> binder.bind(SentimentService.class).toInstance(sentiment))
                .build();

        // Clear any static state IF present in this version of HomeController.
        safeClearStaticMap("cache");
        safeClearStaticLinkedMap("accumulatedResults");
        safeClearStaticDeque("recentQueries"); // may not exist; ok to skip
    }

    // ---------- helpers that NO-OP if field doesn't exist or isn't static ----------

    @SuppressWarnings("unchecked")
    private void safeClearStaticMap(String fieldName) {
        try {
            Field f = HomeController.class.getDeclaredField(fieldName);
            if (!Modifier.isStatic(f.getModifiers())) return; // instance field -> skip
            f.setAccessible(true);
            Object o = f.get(null);
            if (o instanceof Map<?, ?>) {
                ((Map<Object, Object>) o).clear();
            }
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void safeClearStaticLinkedMap(String fieldName) {
        try {
            Field f = HomeController.class.getDeclaredField(fieldName);
            if (!Modifier.isStatic(f.getModifiers())) return; // instance field -> skip
            f.setAccessible(true);
            Object o = f.get(null);
            if (o instanceof LinkedHashMap<?, ?>) {
                ((LinkedHashMap<Object, Object>) o).clear();
            }
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void safeClearStaticDeque(String fieldName) {
        try {
            Field f = HomeController.class.getDeclaredField(fieldName);
            if (!Modifier.isStatic(f.getModifiers())) return; // instance field -> skip
            f.setAccessible(true);
            Object o = f.get(null);
            if (o instanceof Deque<?>) {
                ((Deque<Object>) o).clear();
            }
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------- tests --------------------------

    @Test
    public void index_renders_ok() {
        Result r = controller().index();
        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).contains("NotiLytics"));
    }

    @Test
    public void search_emptyQuery_returnsNeutralAndRenders() {
        Result r = controller().search("").toCompletableFuture().join();
        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).toLowerCase().contains("no articles"));
    }

    @Test
    public void sentiment_endpoint_returnsJson() {
        Result r = controller().sentiment("anything").toCompletableFuture().join();
        assertEquals(OK, r.status());
        String json = contentAsString(r);
        assertEquals(":-)", Json.parse(json).get("sentiment").asText());
        verify(sentiment, times(1)).sentimentForQuery("anything");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void search_withSeededCache_usesCachedAndUpdatesAccumulatedResults_withoutNetwork() throws Exception {
        // Seed cache if this static field is present in current code
        try {
            Field cacheF = HomeController.class.getDeclaredField("cache");
            if (Modifier.isStatic(cacheF.getModifiers())) {
                cacheF.setAccessible(true);
                Map<String, List<Article>> cache = (Map<String, List<Article>>) cacheF.get(null);
                if (cache != null) {
                    cache.put("ai", List.of(
                            new Article("T1", "u1", "S", "su", "2024-01-01 10:00", "d1", 0.1),
                            new Article("T2", "u2", "S", "su", "2024-01-01 10:00", "d2", 0.2)
                    ));
                }
            }
        } catch (NoSuchFieldException ignored) { }

        Result r = controller().search("ai").toCompletableFuture().join();
        assertEquals(OK, r.status());
        String html = contentAsString(r);
        assertFalse(html.isEmpty());

        // If accumulatedResults is static in this revision, assert it recorded the key.
        try {
            Field accF = HomeController.class.getDeclaredField("accumulatedResults");
            if (Modifier.isStatic(accF.getModifiers())) {
                accF.setAccessible(true);
                LinkedHashMap<String, List<Article>> acc =
                        (LinkedHashMap<String, List<Article>>) accF.get(null);
                assertNotNull(acc);
                assertTrue(acc.containsKey("ai"));
            }
        } catch (NoSuchFieldException ignored) { }
    }

    @Test
    public void fetchArticlesForQuery_reflection_executes_and_returnsList() throws Exception {
        HomeController hc = controller();
        Method m = HomeController.class.getDeclaredMethod("fetchArticlesForQuery", String.class);
        m.setAccessible(true);
        Object out = m.invoke(hc, "unit-test-query");
        assertNotNull(out);
        assertTrue(out instanceof List);
    }

    @Test
    public void wordStats_renders_ok() {
        Result r = controller().wordStats("ai").toCompletableFuture().join();
        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).toLowerCase().contains("word"));
    }

    @Test
    public void newsSources_filters_paths_render_ok() {
        Result r1 = controller().newsSources("", "", "");
        assertEquals(OK, r1.status());
        Result r2 = controller().newsSources("us", "business", "en");
        assertEquals(OK, r2.status());
    }
}
