package controllers;

import play.mvc.*;
import utils.ReadabilityUtil;
import views.html.*;
<<<<<<< HEAD
import services.SentimentService;
import javax.inject.Inject;

=======
import models.Article;
>>>>>>> d1ed61c71e71eb4fff1ec0dea7d90bd75d2ce21d
import play.libs.Json;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.*;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class HomeController extends Controller {

    // Cache: query -> list of articles
    private static final Map<String, List<Article>> cache = new ConcurrentHashMap<>();

    // Keep track of last 10 queries globally (simple version)
    private static final Deque<String> recentQueries = new LinkedList<>();

    // Home page
//    public Result index() {
//        return ok(index.render(Collections.<Article>emptyList()));
//    }
    public Result index() {
<<<<<<< HEAD
        return ok(index.render(Collections.<Article>emptyList(), "", ""));
    }

    // Search
//    public Result search(String query) {
//        if (query == null || query.isEmpty()) {
//            return badRequest("Query cannot be empty");
//        }
//
//        // Use cache or fetch new results
//        List<Article> articles = cache.computeIfAbsent(query, this::fetchArticlesForQuery);
//
//        // Track recent queries
//        recentQueries.addFirst(query);
//        if (recentQueries.size() > 10) recentQueries.removeLast();
//
//        // Merge all results from recent queries
//        List<Article> allResults = recentQueries.stream()
//                .flatMap(q -> cache.get(q).stream())
//                .collect(Collectors.toList());
//
//        return ok(index.render(allResults));
//    }
=======
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
>>>>>>> d1ed61c71e71eb4fff1ec0dea7d90bd75d2ce21d

    // Fake API fetch for demonstration
    private List<Article> fetchArticlesForQuery(String query) {
        List<Article> results = new ArrayList<>();

        try {
            String apiKey = System.getenv("NEWSAPI_KEY"); // Replace with your key
            String urlStr = "https://newsapi.org/v2/everything?q="
                    + java.net.URLEncoder.encode(query, "UTF-8")
                    + "&pageSize=50&sortBy=publishedAt&apiKey=" + apiKey;

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
<<<<<<< HEAD


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
    
    /**
     * @author Jaiminkumar Mayani
     */
    
    @Inject private SentimentService sentimentService;   // field injection is fine for Play

    public CompletionStage<Result> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ok(views.html.index.render(List.of(), "", ""))
            );
        }
        /* 1. fetch articles (re-use your existing helper) */
        List<Article> articles = fetchArticlesForQuery(query);

        /* 2. compute sentiment asynchronously */
        return sentimentService.sentimentForQuery(query)
                .thenApply(emoticon ->
                        ok(views.html.index.render(articles, query, emoticon))
                );
    }
    
    public CompletionStage<Result> sentiment(String query) {
        return sentimentService.sentimentForQuery(query)
               .thenApply(emo -> ok(Json.toJson(Map.of("sentiment", emo))));
    }
=======
>>>>>>> d1ed61c71e71eb4fff1ec0dea7d90bd75d2ce21d
}
