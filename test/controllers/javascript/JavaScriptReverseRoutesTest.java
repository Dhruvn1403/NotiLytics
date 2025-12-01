package controllers.javascript;

import org.junit.Test;
import play.api.routing.JavaScriptReverseRoute;
import scala.runtime.AbstractFunction0;

import static org.junit.Assert.*;

/**
 * Tests for JavaScript reverse routes to get coverage on controllers.javascript.
 */
public class JavaScriptReverseRoutesTest {

    // Provide a prefix function for the generated ReverseHomeController
    private final ReverseHomeController routes =
            new ReverseHomeController(new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return "/";   // same as your app’s root prefix
                }
            });

    @Test
    public void index_route_js_generated() {
        JavaScriptReverseRoute r = routes.index();
        String js = r.f();     // Scala val f becomes .f() in Java

        assertNotNull(js);
        assertTrue(js.contains("GET"));
        assertTrue(js.contains("\"/\""));   // root URL
    }

    @Test
    public void search_route_js_generated() {
        JavaScriptReverseRoute r = routes.search();
        String js = r.f();

        assertNotNull(js);
        assertTrue(js.contains("search"));
        assertTrue(js.contains("query"));
        assertTrue(js.contains("sort"));
    }

    @Test
    public void sentiment_route_js_generated() {
        JavaScriptReverseRoute r = routes.sentiment();
        String js = r.f();

        assertNotNull(js);
        assertTrue(js.contains("sentiment"));
        assertTrue(js.contains("query"));
    }

    @Test
    public void wordStats_route_js_generated() {
        JavaScriptReverseRoute r = routes.wordStats();
        String js = r.f();

        assertNotNull(js);
        assertTrue(js.contains("wordstats/"));
        assertTrue(js.contains("encodeURIComponent"));
    }

    @Test
    public void newsSources_route_js_generated() {
        JavaScriptReverseRoute r = routes.newsSources();
        String js = r.f();

        assertNotNull(js);
        assertTrue(js.contains("sources"));
        assertTrue(js.contains("country"));
        assertTrue(js.contains("category"));
        assertTrue(js.contains("language"));
    }

    @Test
    public void ws_route_js_generated() {
        JavaScriptReverseRoute r = routes.ws();
        String js = r.f();

        assertNotNull(js);
        assertTrue(js.contains("ws"));
        assertTrue(js.contains("GET"));
    }
}
