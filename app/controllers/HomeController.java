package controllers;

import models.SourceInfo;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

import services.NewsApiClient;
import services.NewsSources;
import services.SentimentService;

import services.ReadabilityService;

import models.Article;

import play.libs.Json;
import play.libs.streams.ActorFlow;

import com.fasterxml.jackson.databind.JsonNode;

import scala.Tuple2;

import javax.inject.Inject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import views.html.*;

import actors.UserSessionActor;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.actor.typed.javadsl.Adapter;


//    @author Dhruv Patel, Jaiminkumar Mayani, Monil Tailor
public class HomeController extends Controller {

    private static final Map<String, List<Article>> cache = new ConcurrentHashMap<>();
//    private static final Deque<String> recentQueries = new LinkedList<>();

    // Accumulated results for stacked display: keyword -> top 10 articles
    private final LinkedHashMap<String, Tuple2<List<Article>, Double>> accumulatedResults = new LinkedHashMap<>();
    private static final int MAX_KEYWORDS = 10;

    
    private SentimentService sentimentService;
    private NewsApiClient newsApiClient;  

    //    @author Monil Tailor

    private NewsSources NewsSources;
    private ReadabilityService readabilityService;

    // 🔥 ActorSystem injected (required for WebSockets)
    private final ActorSystem classicActorSystem;
    private final Materializer materializer;

    @Inject
    public HomeController(
            ActorSystem classicActorSystem,
            Materializer materializer,
            NewsApiClient newsApiClient,
            NewsSources newsSources,
            SentimentService sentimentService,
            ReadabilityService readabilityService
    ) {
        this.classicActorSystem = classicActorSystem;
        this.materializer = materializer;
        this.newsApiClient = newsApiClient;
        this.NewsSources = newsSources;
        this.sentimentService = sentimentService;
        this.readabilityService = readabilityService;
    }

    //    @author Dhruv Patel
    public Result index() {
        return ok(index.render(new LinkedHashMap<>(), ":-|"));
    }

    //    @author Dhruv Patel
    public CompletionStage<Result> search(String query, String sort) {
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ok(index.render(new LinkedHashMap<>(), ":-|"))
            );
        }

        // Fetch top 10 articles for this query (use cache if available)
        List<Article> articles = cache.computeIfAbsent(query + "_" + sort,
                        key -> fetchArticlesForQuery(query, sort))
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
    private List<Article> fetchArticlesForQuery(String query, String sort) {
        // Your existing fetchArticlesForQuery code remains unchanged
        List<Article> results = new ArrayList<>();
        try {
            String apiKey = "cf69ac0f4dd54ce4a2a5e00503ecaf77";
            String urlStr = "https://newsapi.org/v2/everything?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&pageSize=50&sortBy=" + sort
                    + "&apiKey=" + apiKey;


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

                    double readability = ReadabilityService.calculateReadability(description);
                    results.add(new Article(title, articleUrl, sourceName, sourceUrl,
                            publishedAt, description, readability));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    // ---------------------
    // News Sources (Monil)
    // ---------------------
    public CompletionStage<Result> newsSources(String country, String category, String language) {
        return NewsSources.fetchSources(country, category, language)
                .thenApply(sourcesList -> ok(views.html.newsSources.render(sourcesList)));
    }



    //    @author Jaimin Mayani
    public CompletionStage<Result> sentiment(String query) {
        return sentimentService.sentimentForQuery(query)
                .thenApply(emo -> ok(Json.toJson(Map.of("sentiment", emo))));
    }

    //    @author Varun Oza
    public CompletionStage<Result> wordStats(String query) {
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(badRequest("Query cannot be empty."));
        }

        System.out.println(">>> [WordStats] Generating stats for query: " + query);

        return newsApiClient.searchArticles(query, 50).thenApply(articles -> {
            // Extract all words from article descriptions
            Map<String, Long> wordCounts = articles.stream()
                    .map(a -> a.getDescription() == null ? "" : a.getDescription())
                    .map(desc -> desc.replaceAll("[^a-zA-Z ]", "").toLowerCase())
                    .flatMap(desc -> Arrays.stream(desc.split("\\s+")))
                    .filter(w -> w.length() > 3) // ignore short/common words
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            // Sort descending by frequency
            Map<String, Long> sorted = wordCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(50)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            System.out.println(">>> [WordStats] Computed " + sorted.size() + " words for query: " + query);
            return ok(views.html.wordStats.render(query, sorted));
        });
    }

    public CompletionStage<Result> sourceProfile(String name) {
        return newsApiClient.sourceProfileByName(name)
                .thenApply((SourceInfo info) -> ok(source.render(info)));
    }
    // ============================================================
    //          WEBSOCKET ENDPOINT (DELIVERY 2 ADDITION) 
    // ============================================================

    public WebSocket ws() {
        return WebSocket.Text.accept(request ->
            ActorFlow.actorRef(
                out -> Adapter.props(
                    () -> UserSessionActor.create(
                            Adapter.toTyped(out),
                            newsApiClient,
                            NewsSources,
                            sentimentService,
                            readabilityService
                    )
                ),
                classicActorSystem,
                materializer
            )
        );
    }
}