package services;

import com.fasterxml.jackson.databind.JsonNode;
import models.SourceInfo;
import play.libs.Json;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.typesafe.config.Config;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author Monil Tailor
 *
 * Service class for fetching news sources from NewsAPI.
 * Provides a method to retrieve a list of sources filtered by country, category, and language.
 * Supports asynchronous operation via CompletionStage.
 */
public class NewsSources {
    private final WSClient ws;
    private final Config config;

    @Inject
    public NewsSources(WSClient ws, com.typesafe.config.Config config) {
        this.ws = ws;
        this.config = config;
    }

    private static final String API_KEY = "cf69ac0f4dd54ce4a2a5e00503ecaf77";

    /**
     * Default constructor. Dependency injection supported.
     */
    // @Inject
    // public NewsSources() {
    // }

    /**
     * Single public method replacing the old interface.
     */
    public CompletionStage<List<SourceInfo>> fetchSources(String country,
                                                          String category,
                                                          String language) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder urlStr = new StringBuilder(
                    "https://newsapi.org/v2/sources?apiKey=" + API_KEY
                );

    // if (!country.isEmpty())
    // urlStr.append("&country=").append(country);
    // if (!category.isEmpty())
    // urlStr.append("&category=").append(category);
    // if (!language.isEmpty())
    // urlStr.append("&language=").append(language);

    // URL url = new URL(urlStr.toString());
    // HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    // conn.setRequestMethod("GET");

    // List<SourceInfo> result = new ArrayList<>();

    // if (conn.getResponseCode() == 200) {

    // BufferedReader reader = new BufferedReader(
    // new InputStreamReader(conn.getInputStream()));

    // StringBuilder json = new StringBuilder();
    // String line;

    // while ((line = reader.readLine()) != null) {
    // json.append(line);
    // }

    // reader.close();

    // JsonNode root = Json.parse(json.toString());
    // JsonNode sources = root.get("sources");

    // for (JsonNode s : sources) {
    // SourceInfo info = new SourceInfo(
    // s.path("id").asText(),
    // s.path("name").asText(),
    // s.path("description").asText(),
    // s.path("url").asText(),
    // s.path("category").asText(),
    // s.path("language").asText(),
    // s.path("country").asText(),
    // List.of());
    // result.add(info);
    // }
    // }

    // return result;

    // } catch (Exception e) {
    // e.printStackTrace();
    // return new ArrayList<>();
    // }
    // });
    // }
    public CompletionStage<List<SourceInfo>> fetchSources(String country, String category, String language) {
        if (config.hasPath("newsapi.key")) {
            // Real mode: use WSClient
            String url = "https://newsapi.org/v2/sources?apiKey=" + config.getString("newsapi.key");
            if (!country.isEmpty())
                url += "&country=" + country;
            if (!category.isEmpty())
                url += "&category=" + category;
            if (!language.isEmpty())
                url += "&language=" + language;

            return ws.url(url)
                    .get()
                    .thenApply(resp -> parseSources(resp.asJson()));
        } else {
            // Stub mode: fallback to old hardcoded logic
            return CompletableFuture.completedFuture(List.of(
                    new SourceInfo("id1", "CNN", "desc", "https://cnn.com", "general", "en", "us", List.of())));
        }
    }

    private List<SourceInfo> parseSources(JsonNode root) {
        List<SourceInfo> result = new ArrayList<>();
        JsonNode sources = root.get("sources");
        if (sources != null && sources.isArray()) {
            for (JsonNode s : sources) {
                result.add(new SourceInfo(
                        s.path("id").asText(),
                        s.path("name").asText(),
                        s.path("description").asText(),
                        s.path("url").asText(),
                        s.path("category").asText(),
                        s.path("language").asText(),
                        s.path("country").asText(),
                        List.of()));
            }
        }
        return result;
    }
}
