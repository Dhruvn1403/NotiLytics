package modules;

import org.junit.jupiter.api.Test;
import services.NewsApiClient;
import services.SentimentService;
import static org.junit.jupiter.api.Assertions.*;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author Varun Oza
 */

public class ModuleTest {

    @Test
    void testModuleBindings() {
        Injector injector = Guice.createInjector(new Module());
        NewsApiClient newsClient = injector.getInstance(NewsApiClient.class);
        SentimentService sentiment = injector.getInstance(SentimentService.class);

        assertNotNull(newsClient);
        assertNotNull(sentiment);
    }
}
