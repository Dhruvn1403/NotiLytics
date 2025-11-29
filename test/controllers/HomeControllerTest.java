// /**
//  * @author Java Developers
//  */
// package controllers;

// import models.Article;
// import org.junit.Before;
// import org.junit.Test;
// import play.Application;
// import play.inject.guice.GuiceApplicationBuilder;
// import play.libs.Json;
// import play.mvc.Result;
// import services.SentimentService;

// import java.lang.reflect.Field;
// import java.lang.reflect.Method;
// import java.lang.reflect.Modifier;
// import java.util.*;
// import java.util.concurrent.CompletableFuture;

// import static org.junit.Assert.*;
// import static org.mockito.Mockito.*;
// import static play.test.Helpers.*;

// public class HomeControllerTest {

//     private Application app;
//     private SentimentService sentiment;

//     private HomeController controller() {
//         return app.injector().instanceOf(HomeController.class);
//     }

//     @Before
//     public void setUp() throws Exception {
//         sentiment = mock(SentimentService.class);
//         when(sentiment.sentimentForQuery(anyString()))
//                 .thenAnswer(inv -> CompletableFuture.completedFuture(":-)"));

//         app = new GuiceApplicationBuilder()
//                 .overrides(binder -> binder.bind(SentimentService.class).toInstance(sentiment))
//                 .build();

//         // Clear any static state IF present in this version of HomeController.
//         safeClearStaticMap("cache");
//         safeClearStaticLinkedMap("accumulatedResults");
//         safeClearStaticDeque("recentQueries"); // may not exist; ok to skip
//     }

//     // ---------- helpers that NO-OP if field doesn't exist or isn't static ----------

//     @SuppressWarnings("unchecked")
//     private void safeClearStaticMap(String fieldName) {
//         try {
//             Field f = HomeController.class.getDeclaredField(fieldName);
//             if (!Modifier.isStatic(f.getModifiers())) return; // instance field -> skip
//             f.setAccessible(true);
//             Object o = f.get(null);
//             if (o instanceof Map<?, ?>) {
//                 ((Map<Object, Object>) o).clear();
//             }
//         } catch (NoSuchFieldException ignored) {
//         } catch (Exception e) {
//             throw new RuntimeException(e);
//         }
//     }

//     @SuppressWarnings("unchecked")
//     private void safeClearStaticLinkedMap(String fieldName) {
//         try {
//             Field f = HomeController.class.getDeclaredField(fieldName);
//             if (!Modifier.isStatic(f.getModifiers())) return; // instance field -> skip
//             f.setAccessible(true);
//             Object o = f.get(null);
//             if (o instanceof LinkedHashMap<?, ?>) {
//                 ((LinkedHashMap<Object, Object>) o).clear();
//             }
//         } catch (NoSuchFieldException ignored) {
//         } catch (Exception e) {
//             throw new RuntimeException(e);
//         }
//     }

//     @SuppressWarnings("unchecked")
//     private void safeClearStaticDeque(String fieldName) {
//         try {
//             Field f = HomeController.class.getDeclaredField(fieldName);
//             if (!Modifier.isStatic(f.getModifiers())) return; // instance field -> skip
//             f.setAccessible(true);
//             Object o = f.get(null);
//             if (o instanceof Deque<?>) {
//                 ((Deque<Object>) o).clear();
//             }
//         } catch (NoSuchFieldException ignored) {
//         } catch (Exception e) {
//             throw new RuntimeException(e);
//         }
//     }

//     // -------------------------- tests --------------------------

//     @Test
//     public void index_renders_ok() {
//         Result r = controller().index();
//         assertEquals(OK, r.status());
//         assertTrue(contentAsString(r).contains("NotiLytics"));
//     }

//     @Test
//     public void search_emptyQuery_returnsNeutralAndRenders() {
//         Result r = controller().search("").toCompletableFuture().join();
//         assertEquals(OK, r.status());
//         assertTrue(contentAsString(r).toLowerCase().contains("no articles"));
//     }

//     @Test
//     public void sentiment_endpoint_returnsJson() {
//         Result r = controller().sentiment("anything").toCompletableFuture().join();
//         assertEquals(OK, r.status());
//         String json = contentAsString(r);
//         assertEquals(":-)", Json.parse(json).get("sentiment").asText());
//         verify(sentiment, times(1)).sentimentForQuery("anything");
//     }

//     @Test
//     @SuppressWarnings("unchecked")
//     public void search_withSeededCache_usesCachedAndUpdatesAccumulatedResults_withoutNetwork() throws Exception {
//         // Seed cache if this static field is present in current code
//         try {
//             Field cacheF = HomeController.class.getDeclaredField("cache");
//             if (Modifier.isStatic(cacheF.getModifiers())) {
//                 cacheF.setAccessible(true);
//                 Map<String, List<Article>> cache = (Map<String, List<Article>>) cacheF.get(null);
//                 if (cache != null) {
//                     cache.put("ai", List.of(
//                             new Article("T1", "u1", "S", "su", "2024-01-01 10:00", "d1", 0.1),
//                             new Article("T2", "u2", "S", "su", "2024-01-01 10:00", "d2", 0.2)
//                     ));
//                 }
//             }
//         } catch (NoSuchFieldException ignored) { }

//         Result r = controller().search("ai").toCompletableFuture().join();
//         assertEquals(OK, r.status());
//         String html = contentAsString(r);
//         assertFalse(html.isEmpty());

//         // If accumulatedResults is static in this revision, assert it recorded the key.
//         try {
//             Field accF = HomeController.class.getDeclaredField("accumulatedResults");
//             if (Modifier.isStatic(accF.getModifiers())) {
//                 accF.setAccessible(true);
//                 LinkedHashMap<String, List<Article>> acc =
//                         (LinkedHashMap<String, List<Article>>) accF.get(null);
//                 assertNotNull(acc);
//                 assertTrue(acc.containsKey("ai"));
//             }
//         } catch (NoSuchFieldException ignored) { }
//     }

//     @Test
//     public void fetchArticlesForQuery_reflection_executes_and_returnsList() throws Exception {
//         HomeController hc = controller();
//         Method m = HomeController.class.getDeclaredMethod("fetchArticlesForQuery", String.class);
//         m.setAccessible(true);
//         Object out = m.invoke(hc, "unit-test-query");
//         assertNotNull(out);
//         assertTrue(out instanceof List);
//     }

//     @Test
//     public void wordStats_renders_ok() {
//         Result r = controller().wordStats("ai").toCompletableFuture().join();
//         assertEquals(OK, r.status());
//         assertTrue(contentAsString(r).toLowerCase().contains("word"));
//     }

//     @Test
//     public void newsSources_filters_paths_render_ok() {
//         Result r1 = controller().newsSources("", "", "");
//         assertEquals(OK, r1.status());
//         Result r2 = controller().newsSources("us", "business", "en");
//         assertEquals(OK, r2.status());
//     }
// }


package controllers;

import actors.UserSessionActor;
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

    // mocks
    private SentimentService sentiment;
    private NewsApiClient newsApi;
    private NewsSources newsSources;

    private HomeController controller() {
        return app.injector().instanceOf(HomeController.class);
    }

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

        // Reset static fields in HomeController
        safeClearStaticMap("cache");
    }

    // ---------- Helpers for clearing static fields safely ----------

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

    // ---------------------------- TESTS ----------------------------

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
        assertTrue(contentAsString(r).toLowerCase().contains("notilytics"));
    }

    @Test
    public void sentiment_endpoint_returnsJson() {
        Result r = controller().sentiment("abc").toCompletableFuture().join();
        assertEquals(OK, r.status());
        assertEquals(":-)", Json.parse(contentAsString(r)).get("sentiment").asText());
        verify(sentiment, times(1)).sentimentForQuery("abc");
    }

    @Test
    public void search_withCachedArticles_usesCache_andUpdatesAccumulatedResults() throws Exception {
        // seed cache
        Field f = HomeController.class.getDeclaredField("cache");
        f.setAccessible(true);
        Map<String, List<Article>> cache = (Map<String, List<Article>>) f.get(null);

        cache.put("ai", List.of(
                new Article("T1", "u1", "S", "su", "2024-01-01", "d1", 0.1),
                new Article("T2", "u2", "S", "su", "2024-01-01", "d2", 0.2)
        ));

        Result r = controller().search("ai").toCompletableFuture().join();
        assertEquals(OK, r.status());

        // accumulatedResults is *instance*, not static
        HomeController ctrl = controller();
        Field accF = HomeController.class.getDeclaredField("accumulatedResults");
        accF.setAccessible(true);
        LinkedHashMap<String, ?> acc = (LinkedHashMap<String, ?>) accF.get(ctrl);

        assertTrue(acc.containsKey("ai"));
    }

    @Test
    public void fetchArticlesForQuery_works_withReflection() throws Exception {
        HomeController hc = controller();
        Method m = HomeController.class
                .getDeclaredMethod("fetchArticlesForQuery", String.class);
        m.setAccessible(true);

        Object out = m.invoke(hc, "unit-test");
        assertNotNull(out);
        assertTrue(out instanceof List);
    }

    @Test
    public void wordStats_renders_ok() {
        when(newsApi.searchArticles("ai", 50))
                .thenReturn(CompletableFuture.completedFuture(
                        List.of(new Article("A", "u", "S", "su", "2024-01-01", "Artificial intelligence boom", 0.5))
                ));

        Result r = controller().wordStats("ai").toCompletableFuture().join();
        assertEquals(OK, r.status());
        assertTrue(contentAsString(r).toLowerCase().contains("artificial"));
    }

    @Test
    public void newsSources_renders_ok() {
        Result r1 = controller().newsSources("", "", "").toCompletableFuture().join();
        Result r2 = controller().newsSources("us", "business", "en").toCompletableFuture().join();

        assertEquals(OK, r1.status());
        assertEquals(OK, r2.status());
    }
}
