package services;

import models.Article;
import models.SourceInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class NewsApiClientStub implements NewsApiClient {
    @Override
    public CompletionStage<SourceInfo> sourceProfileByName(String sourceName) {
        var a = new Article(
                "Sample article about " + sourceName,
                "https://example.com/sample",
                sourceName,
                "https://example.com",
                "ZonedDateTime.now()",
                "Demo description",
                0
        );
        var info = new SourceInfo(
                "demo-id", sourceName, "Demo profile for " + sourceName,
                "https://example.com", "general", "en", "us", List.of(a)
        );
        return CompletableFuture.completedFuture(info);
    }
}
