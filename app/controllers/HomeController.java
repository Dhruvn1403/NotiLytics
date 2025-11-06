package controllers;

import play.mvc.*;
import views.html.*;

import play.libs.Json;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class HomeController extends Controller {

    // Cache: query -> list of articles
    private static final Map<String, List<Article>> cache = new ConcurrentHashMap<>();

    // Keep track of last 10 queries globally (simple version)
    private static final Deque<String> recentQueries = new LinkedList<>();

    // Home page
    public Result index() {
        return ok(index.render(Collections.<Article>emptyList()));
    }

    // Search
    public Result search(String query) {
        if (query == null || query.isEmpty()) {
            return badRequest("Query cannot be empty");
        }

        // Use cache or fetch new results
        List<Article> articles = cache.computeIfAbsent(query, this::fetchArticlesForQuery);

        // Track recent queries
        recentQueries.addFirst(query);
        if (recentQueries.size() > 10) recentQueries.removeLast();

        // Merge all results from recent queries
        List<Article> allResults = recentQueries.stream()
                .flatMap(q -> cache.get(q).stream())
                .collect(Collectors.toList());

        return ok(index.render(allResults));
    }

    // Fake API fetch for demonstration
    private List<Article> fetchArticlesForQuery(String query) {
        List<Article> results = new ArrayList<>();

        try {
            String apiKey = "1ede3e954c314c06a3ffb21d639c7c6d"; // Replace with your key
            String urlStr = "https://newsapi.org/v2/everything?q="
                    + java.net.URLEncoder.encode(query, "UTF-8")
                    + "&pageSize=10&sortBy=publishedAt&apiKey=" + apiKey;

            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream())
                );
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
                in.close();

                // Parse JSON using Play's Json library
                JsonNode root = Json.parse(content.toString());
                JsonNode articlesArray = root.get("articles");

                for (JsonNode a : articlesArray) {
                    String title = a.get("title").asText();
                    String articleUrl = a.get("url").asText();
                    String sourceName = a.get("source").get("name").asText();
                    String sourceUrl = "/source/" + a.get("source").get("name").asText(); // NewsAPI doesn't give URL
                    String publishedAt = convertToEDT(
                            LocalDateTime.parse(a.get("publishedAt").asText().replace("Z",""))
                    );

                    results.add(new Article(title, articleUrl, sourceName, sourceUrl, publishedAt));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }


    private String convertToEDT(LocalDateTime time) {
        ZonedDateTime edt = time.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("America/Toronto"));
        return edt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    }

    // Article class
    public static class Article {
        public String title;
        public String url;
        public String sourceName;
        public String sourceUrl;
        public String publishedDate;

        public Article(String title, String url, String sourceName, String sourceUrl, String publishedDate) {
            this.title = title;
            this.url = url;
            this.sourceName = sourceName;
            this.sourceUrl = sourceUrl;
            this.publishedDate = publishedDate;
        }

        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getSourceName() { return sourceName; }
        public String getSourceUrl() { return sourceUrl; }
        public String getPublishedAt() { return publishedDate; }
    }
}
