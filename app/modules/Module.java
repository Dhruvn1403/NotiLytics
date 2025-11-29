package modules;

import com.google.inject.AbstractModule;
import services.NewsApiClient;
import services.NewsSources;
import services.SentimentService;

public class Module extends AbstractModule {
    @Override
    protected void configure() {
        // Bind the interface used in NotiController to its implementation
        // bind(NewsApiClient.class).to(NewsApiClientImpl.class).asEagerSingleton();
        bind(NewsApiClient.class).asEagerSingleton();

        // If you ever want a stub when no key is set, replace the line above with:
        // if (System.getenv("NEWSAPI_KEY") == null || System.getenv("NEWSAPI_KEY").isBlank()) {
        //   bind(NewsApiClient.class).to(services.NewsApiClientStub.class).asEagerSingleton();
        // } else {
        //   bind(NewsApiClient.class).to(NewsApiClientImpl.class).asEagerSingleton();
        // }
        
        /**
         * @author Jaiminkumar Mayani
         */
        bind(SentimentService.class).asEagerSingleton();
        // bind(SentimentService.class).to(SentimentServiceImpl.class).asEagerSingleton();

        /**
         * @author Monil Tailor
         */
        // bind(NewsSources.class).to(NewsSourcesImpl.class).asEagerSingleton();
        bind(NewsSources.class).asEagerSingleton();
    }
}
