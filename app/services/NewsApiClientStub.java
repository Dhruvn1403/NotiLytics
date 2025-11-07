package services;

import models.Article;
import models.SourceInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public class NewsApiClientStub implements NewsApiClient {

    @Override
    public CompletionStage<SourceInfo> sourceProfileByName(String sourceName) {
        var article = new Article(
                "Sample article about " + sourceName,
                "https://example.com/sample",
                sourceName,
                "https://example.com",
                Article.convertToEDT(LocalDateTime.now()),
                "Demo description for testing.",
                0.0
        );

        var info = new SourceInfo(
                "demo-id", sourceName, "Demo profile for " + sourceName,
                "https://example.com", "general", "en", "us", List.of(article)
        );

        return CompletableFuture.completedFuture(info);
    }

    @Override
    public CompletionStage<List<Article>> searchArticles(String query, int limit) {
        List<Article> mockArticles = List.of(
                new Article(
                        "Mock title 1",
                        "https://example.com/1",
                        "MockSource",
                        "https://example.com",
                        Article.convertToEDT(LocalDateTime.now()),
                        "This is a sample article description about AI and technology.",
                        0.0
                ),
                new Article(
                        "Mock title 2",
                        "https://example.com/2",
                        "MockSource",
                        "https://example.com",
                        Article.convertToEDT(LocalDateTime.now()),
                        "Another article mentioning innovation and AI trends.",
                        0.0
                )
        );

        return CompletableFuture.completedFuture(mockArticles);
    }
}
