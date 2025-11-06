package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import controllers.HomeController.Article;
import models.SourceInfo;
import utils.ReadabilityUtil;

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

    private String convertToEDT(LocalDateTime time) {
        ZonedDateTime edt = time.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("America/Toronto"));
        return edt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    }

    /** Attach API key via header and query param. */
    private WSRequest withKey(WSRequest req) {
        // addQueryParameter will encode automatically; do NOT pre-encode
        return req.addHeader("X-Api-Key", apiKey == null ? "" : apiKey)
                .addQueryParameter("apiKey", apiKey == null ? "" : apiKey);
    }

    /** Interface requires this signature. */
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
                                null,
                                sourceName,
                                "Your API key is invalid or incorrect. Check your key, or go to https://newsapi.org to create a free API key.",
                                "", "", "", "", List.of()
                        )
                );
            }

            // Extract matching source
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

            // Make effectively-final copies for use inside lambda
            final String fId = id;
            final String fName = (name == null ? sourceName : name);
            final String fDesc = desc;
            final String fUrl = url;
            final String fCat = cat;
            final String fLang = lang;
            final String fCountry = country;

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
                            String author = a.path("author").asText("");
                            String description = a.path("description").asText("");

                            String publishedAtEt = convertToEDT(
                                    LocalDateTime.parse(a.get("publishedAt").asText().replace("Z",""))
                            );;
                            String published = a.path("publishedAt").asText(null);
                            if (published != null && !published.isBlank()) {
                                try { publishedAtEt = String.valueOf(ZonedDateTime.parse(published)); }
                                catch (Exception ignore) { /* keep null */ }
                            }

                            articles.add(new Article(
                                    title,
                                    aUrl,
                                    fName,
                                    fUrl,
                                    description,
                                    publishedAtEt,
                                    ReadabilityUtil.calculateReadability(description)
                            ));
                        }
                    }
                }
                return new SourceInfo(fId, fName, fDesc, fUrl, fCat, fLang, fCountry, articles);
            });
        });
    }

    // ---- helpers ----
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
