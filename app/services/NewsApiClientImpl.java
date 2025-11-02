package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import models.Article;
import models.SourceInfo;

import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import util.TimeUtil;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class NewsApiClientImpl implements NewsApiClient {

    private final WSClient ws;
    private final String baseUrl;
    private final String apiKey;

    @Inject
    public NewsApiClientImpl(WSClient ws, Config config) {
        this.ws = ws;
        this.baseUrl = config.hasPath("newsapi.baseUrl")
                ? config.getString("newsapi.baseUrl")
                : "https://newsapi.org/v2";
        this.apiKey = config.hasPath("newsapi.apiKey")
                ? config.getString("newsapi.apiKey")
                : "";
    }

    @Override
    public CompletionStage<SourceInfo> sourceProfileByName(String sourceName) {
        WSRequest reqSources = ws.url(baseUrl + "/top-headlines/sources")
                .addHeader("X-Api-Key", apiKey);

        return reqSources.get()
                .thenApply(WSResponse::asJson)
                .thenCompose(json -> {
                    if (json.has("status") && "error".equalsIgnoreCase(json.get("status").asText())) {
                        return java.util.concurrent.CompletableFuture.completedFuture(
                                new SourceInfo(null, sourceName,
                                        json.has("message") ? json.get("message").asText() : "Not found",
                                        "", "", "", "", List.of())
                        );
                    }

                    JsonNode sources = json.get("sources");
                    JsonNode match = (sources == null) ? null :
                            StreamSupport.stream(sources.spliterator(), false)
                                    .filter(n -> n.hasNonNull("name")
                                            && n.get("name").asText("").equalsIgnoreCase(sourceName))
                                    .findFirst().orElse(null);

                    String id, desc, url, cat, lang, country;

                    if (match == null) {
                        id      = sourceName.toLowerCase(Locale.ROOT).replace(' ', '-');
                        desc    = "Not found";
                        url     = "";
                        cat     = "";
                        lang    = "";
                        country = "";
                    } else {
                        id      = text(match, "id");
                        desc    = text(match, "description");
                        url     = text(match, "url");
                        cat     = text(match, "category");
                        lang    = text(match, "language");
                        country = text(match, "country");
                    }

                    WSRequest reqArticles = ws.url(baseUrl + "/everything")
                            .addHeader("X-Api-Key", apiKey)
                            .addQueryParameter("sources", id)
                            .addQueryParameter("pageSize", "10")
                            .addQueryParameter("sortBy", "publishedAt");

                    return reqArticles.get()
                            .thenApply(WSResponse::asJson)
                            .thenApply(articlesJson -> {
                                if (articlesJson.has("status")
                                        && "error".equalsIgnoreCase(articlesJson.get("status").asText())) {
                                    return new SourceInfo(id, sourceName, desc, url, cat, lang, country, List.of());
                                }

                                JsonNode articlesArr = articlesJson.get("articles");
                                List<Article> articles = (articlesArr == null) ? List.of() :
                                        StreamSupport.stream(articlesArr.spliterator(), false)
                                                .map(a -> {
                                                    String title  = text(a, "title");
                                                    String link   = text(a, "url");
                                                    String sName  = a.path("source").path("name").asText("");
                                                    String author = text(a, "author");
                                                    String descA  = text(a, "description");
                                                    String pub    = text(a, "publishedAt");
                                                    ZonedDateTime et = pub.isBlank() ? null : TimeUtil.toET(pub);
                                                    return new Article(title, link, sName, host(link), author, descA, et);
                                                })
                                                .collect(Collectors.toList());

                                return new SourceInfo(id, sourceName, desc, url, cat, lang, country, articles);
                            });
                });
    }

    private static String text(JsonNode n, String key) {
        return (n != null && n.hasNonNull(key)) ? n.get(key).asText("") : "";
    }

    private static String host(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getProtocol() + "://" + u.getHost();
        } catch (Exception e) {
            return "";
        }
    }
}
