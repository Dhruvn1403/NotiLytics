package controllers;

import play.mvc.*;
import utils.ReadabilityUtil;
import views.html.*;
import services.SentimentService;
import javax.inject.Inject;
import models.Article;
import play.libs.Json;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//    @author Dhruv Patel, Jaimim Mayani
public class HomeController extends Controller {

    private static final Map<String, List<Article>> cache = new ConcurrentHashMap<>();
    private static final Deque<String> recentQueries = new LinkedList<>();

    // Accumulated results for stacked display: keyword -> top 10 articles
    private static final LinkedHashMap<String, List<Article>> accumulatedResults = new LinkedHashMap<>();
    private static final int MAX_KEYWORDS = 10;

    @Inject
    private SentimentService sentimentService;

    //    @author Dhruv Patel
    public Result index() {
        return ok(index.render(new LinkedHashMap<String, List<Article>>(), ":-|"));
    }

    //    @author Dhruv Patel
    public CompletionStage<Result> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ok(index.render(new LinkedHashMap<String, List<Article>>(), ":-|"))
            );
        }

        // Fetch top 10 articles for this query (use cache if available)
        List<Article> articles = cache.computeIfAbsent(query, this::fetchArticlesForQuery)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        // Add to accumulatedResults (newest keyword on top)
        accumulatedResults.put(query, articles);

        // Remove oldest keyword if more than MAX_KEYWORDS
        if (accumulatedResults.size() > MAX_KEYWORDS) {
            String oldestKey = accumulatedResults.keySet().iterator().next();
            accumulatedResults.remove(oldestKey);
        }

        // Convert to LinkedHashMap before passing to the view
        LinkedHashMap<String, List<Article>> linkedResults = new LinkedHashMap<>(accumulatedResults);

        // Fetch sentiment for this query
        return sentimentService.sentimentForQuery(query)
                .thenApply(emoticon -> ok(index.render(linkedResults, emoticon)));
    }

    //    @author Dhruv Patel
    private List<Article> fetchArticlesForQuery(String query) {
        // Your existing fetchArticlesForQuery code remains unchanged
        List<Article> results = new ArrayList<>();
        try {
            String apiKey = "197e644898e24b5384081402fdaafcd3";
            String urlStr = "https://newsapi.org/v2/everything?q="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&pageSize=50&sortBy=publishedAt&apiKey=" + apiKey;

            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream())
                );
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) content.append(line);
                in.close();

                JsonNode root = Json.parse(content.toString());
                JsonNode articlesArray = root.get("articles");

                int limit = Math.min(10, articlesArray.size());
                for (int i = 0; i < limit; i++) {
                    JsonNode a = articlesArray.get(i);
                    String title = a.path("title").asText("");
                    String articleUrl = a.path("url").asText("");
                    String sourceName = a.path("source").path("name").asText("");
                    String sourceUrl = "/source/" + sourceName.replaceAll("\\s+", "_");
                    String publishedAtUtc = a.path("publishedAt").asText("");
                    String publishedAt = publishedAtUtc;
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                        LocalDateTime utcTime = LocalDateTime.parse(publishedAtUtc.replace("Z", ""), formatter);
                        publishedAt = Article.convertToEDT(utcTime);
                    } catch (Exception e) {
                        publishedAt = publishedAtUtc;
                    }
                    String description = a.hasNonNull("description")
                            ? a.get("description").asText()
                            : "No description available";

                    double readability = ReadabilityUtil.calculateReadability(description);
                    results.add(new Article(title, articleUrl, sourceName, sourceUrl,
                            publishedAt, description, readability));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    //    @author Jaimin Mayani
    public CompletionStage<Result> sentiment(String query) {
        return sentimentService.sentimentForQuery(query)
                .thenApply(emo -> ok(Json.toJson(Map.of("sentiment", emo))));
    }
}
