package controllers;

import play.mvc.*;
import utils.ReadabilityUtil;
import views.html.*;
import models.Article;
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
        return ok(index.render(Collections.emptyList(), ""));
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
                .toList();

        return ok(views.html.index.render(articles, query));
    }

    // Fake API fetch for demonstration
    private List<Article> fetchArticlesForQuery(String query) {
        List<Article> results = new ArrayList<>();

        try {
            String apiKey = System.getenv("NEWSAPI_KEY"); // Replace with your key
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
                    String publishedAt = Article.convertToEDT(
                            LocalDateTime.parse(a.get("publishedAt").asText().replace("Z",""))
                    );
                    String description = a.hasNonNull("description") ? a.get("description").asText() : "No description available";
                    double readability = ReadabilityUtil.calculateReadability(description);

                    results.add(new Article(title, articleUrl, sourceName, sourceUrl, publishedAt, description, readability));

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
