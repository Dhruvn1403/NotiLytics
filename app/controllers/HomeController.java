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

/**
 * Main controller for the News Dashboard application.
 * Handles search, sentiment analysis, word statistics, source information, and WebSocket endpoints.
 * <p>
 * Accumulated search results and caching are implemented for efficiency.
 * </p>
 *
 * Authors:
 * - Dhruv Patel (Readability, search handling)
 * - Monil Tailor (News sources)
 * - Manush Shah (Source Profile)
 * - Jaimin Mayani (Article sentiment)
 * - Varun Oza (Word statistics)
 * - Group (WebSocket & session integration)
 */
public class HomeController extends Controller {

    private static final Map<String, List<Article>> cache = new ConcurrentHashMap<>();
    private final LinkedHashMap<String, Tuple2<List<Article>, Double>> accumulatedResults = new LinkedHashMap<>();
    private static final int MAX_KEYWORDS = 10;

    private final SentimentService sentimentService;
    private final NewsApiClient newsApiClient;
    private final NewsSources NewsSources;
    private final ReadabilityService readabilityService;

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

    /**
     * Renders the homepage.
     *
     * @return OK result with empty index view
     * @author Dhruv Patel
     */
    public Result index() {
        return ok(index.render(new LinkedHashMap<>(), ":-|"));
    }

    /**
     * Handles search requests, fetches top articles, calculates readability,
     * updates accumulated results, and fetches sentiment.
     *
     * @param query the search keyword
     * @param sort  the sort order for articles
     * @return CompletionStage of Result with rendered view
     * @author Dhruv Patel
     */
    public CompletionStage<Result> search(String query, String sort) {
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(ok(index.render(new LinkedHashMap<>(), ":-|")));
        }

        List<Article> articles = cache.computeIfAbsent(query + "_" + sort,
                        key -> fetchArticlesForQuery(query, sort))
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        double avgReadability = articles.stream()
                .mapToDouble(Article::getReadabilityScore)
                .average()
                .orElse(0.0);

        accumulatedResults.put(query, new Tuple2<>(articles, avgReadability));

        if (accumulatedResults.size() > MAX_KEYWORDS) {
            String oldestKey = accumulatedResults.keySet().iterator().next();
            accumulatedResults.remove(oldestKey);
        }

        return sentimentService.sentimentForQuery(query)
                .thenApply(emoticon -> ok(index.render(accumulatedResults, emoticon)));
    }

    /**
     * Fetches articles from NewsAPI.org for the given query and sort order.
     *
     * @param query the search keyword
     * @param sort  the sort order for results
     * @return list of Article objects
     * @author Dhruv Patel
     */
    private List<Article> fetchArticlesForQuery(String query, String sort) {
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

    /**
     * Fetches news sources filtered by country, category, and language.
     *
     * @param country  the country code
     * @param category the news category
     * @param language the language code
     * @return CompletionStage of Result rendering sources view
     * @author Monil Tailor
     */
    public CompletionStage<Result> newsSources(String country, String category, String language) {
        return NewsSources.fetchSources(country, category, language)
                .thenApply(sourcesList -> ok(views.html.newsSources.render(sourcesList)));
    }

    /**
     * Returns sentiment for a given query.
     *
     * @param query the search keyword
     * @return CompletionStage of Result containing sentiment JSON
     * @author Jaimin Mayani
     */
    public CompletionStage<Result> sentiment(String query) {
        return sentimentService.sentimentForQuery(query)
                .thenApply(emo -> ok(Json.toJson(Map.of("sentiment", emo))));
    }

    /**
     * Generates word statistics for a query by analyzing article descriptions.
     *
     * @param query the search keyword
     * @return CompletionStage of Result rendering word stats view
     * @author Varun Oza
     */
    public CompletionStage<Result> wordStats(String query) {
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(badRequest("Query cannot be empty."));
        }

        System.out.println(">>> [WordStats] Generating stats for query: " + query);

        return newsApiClient.searchArticles(query, 50).thenApply(articles -> {
            Map<String, Long> wordCounts = articles.stream()
                    .map(a -> a.getDescription() == null ? "" : a.getDescription())
                    .map(desc -> desc.replaceAll("[^a-zA-Z ]", "").toLowerCase())
                    .flatMap(desc -> Arrays.stream(desc.split("\\s+")))
                    .filter(w -> w.length() > 3)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

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

    /**
     * Returns a profile of a specific news source.
     *
     * @param name the source name
     * @return CompletionStage of Result rendering source profile view
     * @author Manush Shah
     */
    public CompletionStage<Result> sourceProfile(String name) {
        return newsApiClient.sourceProfileByName(name)
                .thenApply((SourceInfo info) -> ok(source.render(info)));
    }

    /**
     * WebSocket endpoint for live session updates.
     * Integrates UserSessionActor for managing session state and pushing updates to the client.
     *
     * @return WebSocket accepting text messages
     * @author Group
     */
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
