package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import models.Article;
import models.SourceInfo;

/**
 * Implementation of NewsApiClient for live data fetches from NewsAPI.org.
 * Converts publish time to Eastern Time (EST/EDT) and computes readability.
 *
 * @author Varun Oza
 */
public final class NewsApiClientImpl implements NewsApiClient {

    private final WSClient ws;
    private final String baseUrl;
    private final String apiKey;

    @Inject
    public NewsApiClientImpl(WSClient ws, Config config) {
        this.ws = ws;
        this.baseUrl = "https://newsapi.org/v2";

        String fromEnv = System.getenv("NEWSAPI_KEY");
        this.apiKey = (fromEnv != null && !fromEnv.isBlank())
                ? fromEnv
                : (config.hasPath("newsapi.key") ? config.getString("newsapi.key") : null);

        System.out.println("NEWSAPI_KEY present? " + (this.apiKey != null && !this.apiKey.isBlank()));
    }

    /** Attach API key via header and query param. */
    private WSRequest withKey(WSRequest req) {
        return req.addHeader("X-Api-Key", apiKey == null ? "" : apiKey)
                  .addQueryParameter("apiKey", apiKey == null ? "" : apiKey);
    }

    /** Source Profile (team feature). */
    @Override
    public CompletionStage<SourceInfo> sourceProfileByName(String sourceName) {
        WSRequest findReq = withKey(ws.url(baseUrl + "/top-headlines/sources")
                .addQueryParameter("language", "en")
                .addQueryParameter("country", "us"));

        return findReq.get().thenCompose(resp -> {
            JsonNode root = handle(resp, "lookup-sources");
            if (root == null) {
                return java.util.concurrent.CompletableFuture.completedFuture(
                        new SourceInfo(
                                null, sourceName,
                                "Your API key is invalid or incorrect.",
                                "", "", "", "", List.of()
                        )
                );
            }

            JsonNode sources = root.path("sources");
            String id = null, name = null, desc = null, url = "", cat = "", lang = "", country = "";
            if (sources.isArray()) {
                for (JsonNode s : sources) {
                    String n = s.path("name").asText("");
                    if (n.equalsIgnoreCase(sourceName)) {
                        id = s.path("id").asText("");
                        name = n;
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
                return java.util.concurrent.CompletableFuture.completedFuture(
                        new SourceInfo(null, sourceName, "Not found", "", "", "", "", List.of())
                );
            }

            final String fId = id, fName = name, fDesc = desc, fUrl = url, fCat = cat, fLang = lang, fCountry = country;

            WSRequest artReq = withKey(ws.url(baseUrl + "/everything")
                    .addQueryParameter("sources", fId)
                    .addQueryParameter("pageSize", "10")
                    .addQueryParameter("sortBy", "publishedAt"));

            return artReq.get().thenApply(artResp -> {
                JsonNode artRoot = handle(artResp, "fetch-articles");
                List<Article> articles = new ArrayList<>();
                if (artRoot != null) {
                    JsonNode arr = artRoot.path("articles");
                    if (arr.isArray()) {
                        for (JsonNode a : arr) {
                            String title = a.path("title").asText("");
                            String aUrl = a.path("url").asText("");
                            String description = a.path("description").asText("");
                            String publishedAtUtc = a.path("publishedAt").asText("");

                            // Convert UTC to EDT
                            String publishedAtEt = "";
                            try {
                                publishedAtEt = Article.convertToEDT(
                                        LocalDateTime.parse(publishedAtUtc.replace("Z", ""))
                                );
                            } catch (Exception e) {
                                publishedAtEt = publishedAtUtc;
                            }

                            articles.add(new Article(
                                    title,
                                    aUrl,
                                    fName,
                                    fUrl,
                                    publishedAtEt,
                                    description,
                                    0.0  // placeholder readability
                            ));
                        }
                    }
                }
                return new SourceInfo(fId, fName, fDesc, fUrl, fCat, fLang, fCountry, articles);
            });
        });
    }

    /** Search Articles (used by WordStats feature). */
    @Override
    public CompletionStage<List<Article>> searchArticles(String query, int limit) {
        WSRequest req = withKey(ws.url(baseUrl + "/everything")
                .addQueryParameter("q", "\"" + query + "\"")
                .addQueryParameter("pageSize", String.valueOf(limit))
                .addQueryParameter("sortBy", "publishedAt")
                .addQueryParameter("language", "en"));

        return req.get().thenApply(resp -> {
            JsonNode root = handle(resp, "wordstats-search");
            List<Article> articles = new ArrayList<>();

            if (root != null) {
                JsonNode arr = root.path("articles");
                if (arr.isArray()) {
                    for (JsonNode a : arr) {
                        String title = a.path("title").asText("");
                        String url = a.path("url").asText("");
                        String sourceName = a.path("source").path("name").asText("");
                        String sourceUrl = "";
                        String description = a.path("description").asText("");
                        String publishedAtUtc = a.path("publishedAt").asText("");

                        // Convert UTC to EDT
                        String publishedAtEt = "";
                        try {
                            publishedAtEt = Article.convertToEDT(
                                    LocalDateTime.parse(publishedAtUtc.replace("Z", ""))
                            );
                        } catch (Exception e) {
                            publishedAtEt = publishedAtUtc;
                        }

                        articles.add(new Article(
                                title,
                                url,
                                sourceName,
                                sourceUrl,
                                publishedAtEt,
                                description,
                                0.0
                        ));
                    }
                }
            }
            return articles;
        });
    }

    // --- Helper methods ---

    private JsonNode handle(WSResponse resp, String tag) {
        int status = resp.getStatus();
        JsonNode body = safeJson(resp);
        String statusText = body != null ? body.path("status").asText("") : "";

        if (status >= 200 && status < 300 && "ok".equalsIgnoreCase(statusText)) {
            return body;
        }

        String code = body != null ? body.path("code").asText("") : "";
        String message = body != null ? body.path("message").asText("") : "";
        System.err.println("[NewsAPI/" + tag + "] HTTP " + status +
                " status=" + statusText + " code=" + code + " msg=" + message);
        return null;
    }

    private JsonNode safeJson(WSResponse resp) {
        try { return resp.asJson(); }
        catch (Exception e) {
            System.err.println("[NewsAPI] Non-JSON response: " + resp.getStatusText());
            return null;
        }
    }
}
