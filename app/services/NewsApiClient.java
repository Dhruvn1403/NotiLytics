package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import models.Article;
import models.SourceInfo;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author group
 *
 * NewsApiClient provides a unified client for interacting with NewsAPI.org.
 * It supports both real API calls (with a valid API key) and stubbed responses for testing.
 *
 * Features:
 * - Fetch articles for a search query
 * - Fetch news source profiles
 * - Stub mode if API key is missing
 *
 * Dependencies:
 * - WSClient for HTTP requests
 * - Config for application configuration
 */
public final class NewsApiClient {

    private final WSClient ws;
    private final String baseUrl;
    private final String apiKey;
    private final boolean useStub;

    /**
     * Constructor: initializes the client with WSClient and configuration.
     * Automatically enables stub mode if no API key is found.
     *
     * @param ws WSClient for HTTP calls
     * @param config Application configuration
     */
    @Inject
    public NewsApiClient(WSClient ws, Config config) {
        this.ws = ws;
        this.baseUrl = "https://newsapi.org/v2";

        String fromEnv = System.getenv("NEWSAPI_KEY");
        this.apiKey = (fromEnv != null && !fromEnv.isBlank())
                ? fromEnv
                : (config.hasPath("newsapi.key") ? config.getString("newsapi.key") : null);

        this.useStub = (apiKey == null || apiKey.isBlank());

        System.out.println("NEWSAPI_KEY present? "
                + (this.apiKey != null && !this.apiKey.isBlank())
                + " (stub mode? " + useStub + ")");
    }


    /** ---------------------- STUB MODE ------------------------- */

    /**
     * Returns a stubbed SourceInfo for testing purposes.
     *
     * @param sourceName Name of the source
     * @return CompletionStage<SourceInfo> with mock data
     */
    private CompletionStage<SourceInfo> stubSourceProfile(String sourceName) {
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

    /**
     * Returns a stubbed list of articles for testing purposes.
     *
     * @param query Search query
     * @param limit Number of articles
     * @return CompletionStage<List<Article>> with mock articles
     */
    private CompletionStage<List<Article>> stubSearch(String query, int limit) {
        List<Article> mock = List.of(
                new Article(
                        "Mock title 1",
                        "https://example.com/1",
                        "MockSource",
                        "https://example.com",
                        Article.convertToEDT(LocalDateTime.now()),
                        "Sample article description about AI.",
                        0.0
                ),
                new Article(
                        "Mock title 2",
                        "https://example.com/2",
                        "MockSource",
                        "https://example.com",
                        Article.convertToEDT(LocalDateTime.now()),
                        "Another mock article about innovation.",
                        0.0
                )
        );

        return CompletableFuture.completedFuture(mock);
    }


    /** ---------------------- REAL IMPLEMENTATION ------------------------- */

    /**
     * Adds the API key to the request headers and query parameters.
     * @param req WSRequest
     * @return WSRequest with API key added
     */
    private WSRequest withKey(WSRequest req) {
        return req.addHeader("X-Api-Key", apiKey)
                  .addQueryParameter("apiKey", apiKey);
    }

    /**
     * Fetches a news source profile by its name.
     * Uses stub mode if API key is missing.
     *
     * @param sourceName Name of the news source
     * @return CompletionStage<SourceInfo> with source info and top articles
     */
    public CompletionStage<SourceInfo> sourceProfileByName(String sourceName) {

        if (useStub) return stubSourceProfile(sourceName);

        WSRequest findReq = withKey(ws.url(baseUrl + "/top-headlines/sources")
                .addQueryParameter("language", "en")
                .addQueryParameter("country", "us"));

        return findReq.get().thenCompose(resp -> {
            JsonNode root = handle(resp);
            if (root == null)
                return CompletableFuture.completedFuture(
                        new SourceInfo(null, sourceName,
                                "API key invalid or request failed.",
                                "", "", "", "", List.of())
                );

            JsonNode sources = root.path("sources");

            String id = null, name = null, desc = null, url = "", cat = "", lang = "", country = "";

            if (sources.isArray()) {
                for (JsonNode s : sources) {
                    if (s.path("name").asText("").equalsIgnoreCase(sourceName)) {
                        id = s.path("id").asText("");
                        name = s.path("name").asText("");
                        desc = s.path("description").asText("");
                        url = s.path("url").asText("");
                        cat = s.path("category").asText("");
                        lang = s.path("language").asText("");
                        country = s.path("country").asText("");
                        break;
                    }
                }
            }

            if (id == null || id.isBlank()) {
                return CompletableFuture.completedFuture(
                        new SourceInfo(null, sourceName, "Not found",
                                "", "", "", "", List.of())
                );
            }

            final String fId = id, fName = name, fDesc = desc, fUrl = url,
                         fCat = cat, fLang = lang, fCountry = country;

            WSRequest artReq = withKey(ws.url(baseUrl + "/everything")
                    .addQueryParameter("sources", fId)
                    .addQueryParameter("pageSize", "10")
                    .addQueryParameter("sortBy", "publishedAt"));

            return artReq.get().thenApply(artResp -> {
                JsonNode artRoot = handle(artResp);

                List<Article> articles = new ArrayList<>();
                if (artRoot != null) {
                    JsonNode arr = artRoot.path("articles");
                    if (arr.isArray()) {
                        for (JsonNode a : arr) {
                            String title = a.path("title").asText("");
                            String aUrl = a.path("url").asText("");
                            String description = a.path("description").asText("");
                            String publishedAtUtc = a.path("publishedAt").asText("");

                            String publishedAtEt;
                            try {
                                publishedAtEt = Article.convertToEDT(
                                        LocalDateTime.parse(publishedAtUtc.replace("Z", "")));
                            } catch (Exception e) {
                                publishedAtEt = publishedAtUtc;
                            }

                            articles.add(new Article(
                                    title, aUrl, fName, fUrl,
                                    publishedAtEt, description, 0.0
                            ));
                        }
                    }
                }

                return new SourceInfo(fId, fName, fDesc, fUrl,
                        fCat, fLang, fCountry, articles);
            });
        });
    }


    /**
     * Searches for articles based on query and limit.
     * Uses stub mode if API key is missing.
     *
     * @param query Search query
     * @param limit Maximum number of articles to fetch
     * @return CompletionStage<List<Article>> list of articles
     */
    public CompletionStage<List<Article>> searchArticles(String query, int limit) {

        if (useStub) return stubSearch(query, limit);

        WSRequest req = withKey(ws.url(baseUrl + "/everything")
                .addQueryParameter("q", "\"" + query + "\"")
                .addQueryParameter("pageSize", String.valueOf(limit))
                .addQueryParameter("sortBy", "publishedAt")
                .addQueryParameter("language", "en"));

        return req.get().thenApply(resp -> {
            JsonNode root = handle(resp);

            List<Article> articles = new ArrayList<>();
            if (root != null) {
                JsonNode arr = root.path("articles");
                if (arr.isArray()) {
                    for (JsonNode a : arr) {
                        String title = a.path("title").asText("");
                        String url = a.path("url").asText("");
                        String sourceName = a.path("source").path("name").asText("");
                        String description = a.path("description").asText("");
                        String publishedAtUtc = a.path("publishedAt").asText("");

                        String publishedAtEt;
                        try {
                            publishedAtEt = Article.convertToEDT(
                                    LocalDateTime.parse(publishedAtUtc.replace("Z", "")));
                        } catch (Exception e) {
                            publishedAtEt = publishedAtUtc;
                        }

                        articles.add(new Article(
                                title, url, sourceName, "",
                                publishedAtEt, description, 0.0
                        ));
                    }
                }
            }

            return articles;
        });
    }


    /** ---------------------- Helper Methods ------------------------- */

    /**
     * Validates HTTP response from NewsAPI and extracts JSON if valid.
     * @param resp WSResponse
     * @return JsonNode or null if invalid
     */
    private JsonNode handle(WSResponse resp) {
        int status = resp.getStatus();
        JsonNode body = safeJson(resp);
        String statusText = body != null ? body.path("status").asText("") : "";

        if (status >= 200 && status < 300 && "ok".equalsIgnoreCase(statusText)) {
            return body;
        }

        System.err.println("[NewsAPI] HTTP " + status + " Response invalid.");
        return null;
    }

    private JsonNode safeJson(WSResponse resp) {
        try { return resp.asJson(); }
        catch (Exception e) {
            System.err.println("[NewsAPI] Non-JSON response.");
            return null;
        }
    }
}
