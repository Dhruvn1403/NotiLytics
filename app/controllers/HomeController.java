package controllers;

import play.mvc.*;
import scala.Tuple2;
import utils.ReadabilityUtil;
import views.html.*;
import services.SentimentService;
import javax.inject.Inject;
import models.Article;
import play.libs.Json;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//    @author Dhruv Patel, Jaimin Mayani, Monil Tailor
public class HomeController extends Controller {

    private static final Map<String, List<Article>> cache = new ConcurrentHashMap<>();
//    private static final Deque<String> recentQueries = new LinkedList<>();

    // Accumulated results for stacked display: keyword -> top 10 articles
    private final LinkedHashMap<String, Tuple2<List<Article>, Double>> accumulatedResults = new LinkedHashMap<>();
    private static final int MAX_KEYWORDS = 10;

    @Inject
    private SentimentService sentimentService;

    //    @author Dhruv Patel
    public Result index() {
        return ok(index.render(new LinkedHashMap<>(), ":-|"));
    }

    //    @author Dhruv Patel
    public CompletionStage<Result> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ok(index.render(new LinkedHashMap<>(), ":-|"))
            );
        }

        // Fetch top 10 articles for this query (use cache if available)
        List<Article> articles = cache.computeIfAbsent(query, this::fetchArticlesForQuery)
                .stream()
                .limit(10)
                .collect(Collectors.toList());
//        System.out.println(articles);
        // Calc Average Readability
        double avgReadability = articles.stream()
                .mapToDouble(Article::getReadabilityScore)
                .average()
                .orElse(0.0);


        // Add to accumulatedResults (newest keyword on top)
        accumulatedResults.put(query, new Tuple2<>(articles, avgReadability));

        // Remove oldest keyword if more than MAX_KEYWORDS
        if (accumulatedResults.size() > MAX_KEYWORDS) {
            String oldestKey = accumulatedResults.keySet().iterator().next();
            accumulatedResults.remove(oldestKey);
        }

        // Convert to LinkedHashMap before passing to the view
//        LinkedHashMap<String, Map.Entry<List<Article>, Double>> linkedResults =
//                new LinkedHashMap<>(accumulatedResults);


        // Fetch sentiment for this query
        return sentimentService.sentimentForQuery(query)
                .thenApply(emoticon -> ok(index.render(accumulatedResults, emoticon)));
    }

    //    @author Dhruv Patel
    private List<Article> fetchArticlesForQuery(String query) {
        // Your existing fetchArticlesForQuery code remains unchanged
        List<Article> results = new ArrayList<>();
        try {
            String apiKey = "cf69ac0f4dd54ce4a2a5e00503ecaf77";
            String urlStr = "https://newsapi.org/v2/everything?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
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

    //    @author Monil Tailor
    public Result newsSources(String country, String category, String language) {
        try {
            String apiKey = "197e644898e24b5384081402fdaafcd3";
            StringBuilder urlStr = new StringBuilder("https://newsapi.org/v2/sources?apiKey=" + apiKey);

            if (country != null && !country.isEmpty()) urlStr.append("&country=").append(country);
            if (category != null && !category.isEmpty()) urlStr.append("&category=").append(category);
            if (language != null && !language.isEmpty()) urlStr.append("&language=").append(language);

            java.net.URL url = new java.net.URL(urlStr.toString());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            List<Map<String, String>> sourcesList = new ArrayList<>();
            if (conn.getResponseCode() == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream())
                );
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) content.append(line);
                in.close();

                JsonNode root = Json.parse(content.toString());
                JsonNode sources = root.get("sources");

                for (JsonNode s : sources) {
                    Map<String, String> sourceInfo = new HashMap<>();
                    sourceInfo.put("id", s.path("id").asText());
                    sourceInfo.put("name", s.path("name").asText());
                    sourceInfo.put("description", s.path("description").asText(""));
                    sourceInfo.put("url", s.path("url").asText(""));
                    sourceInfo.put("category", s.path("category").asText(""));
                    sourceInfo.put("language", s.path("language").asText(""));
                    sourceInfo.put("country", s.path("country").asText(""));
                    sourcesList.add(sourceInfo);
                }
            }

            return ok(views.html.newsSources.render(sourcesList));
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("Failed to fetch news sources");
        }
    }

    //    @author Jaimin Mayani
    public CompletionStage<Result> sentiment(String query) {
        return sentimentService.sentimentForQuery(query)
                .thenApply(emo -> ok(Json.toJson(Map.of("sentiment", emo))));
    }
}
