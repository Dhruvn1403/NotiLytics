package modules;

import com.google.inject.AbstractModule;
import services.NewsApiClient;
import services.NewsApiClientImpl;

public class Module extends AbstractModule {
    @Override
    protected void configure() {
        bind(NewsApiClient.class).to(NewsApiClientImpl.class);
    }
}
