package modules;

import com.google.inject.AbstractModule;
import services.NewsApiClient;
import services.NewsApiClientStub;  // use the stub first so everything runs

public class Module extends AbstractModule {
    @Override
    protected void configure() {
        bind(NewsApiClient.class).to(NewsApiClientStub.class);
    }
}
