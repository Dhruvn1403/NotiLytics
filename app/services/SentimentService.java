package services;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * SentimentService class to determine the overall sentiment of news articles
 * related to a given query. Returns one of three emoticons:
 * ":-)" for positive sentiment, ":-(" for negative, or ":-|" for neutral.
 *
 * Uses a simple heuristic approach with predefined happy and sad word sets.
 *
 * @author Jaiminkumar Mayani
 */
@Singleton
public final class SentimentService {

    private final WSClient ws;
    private final String apiKey;

    /* ---------- Predefined happy / sad word sets ---------- */
    private static final Set<String> HAPPY = Set.of(
            "joy", "happy", "excited", "love", "amazing", "awesome", "fantastic",
            "great", "wonderful", "good", "best", "win", "victory", "celebrate",
            "😊", "😄", "😍", "🎉", "❤️", "👍", ":)", ":-)", "lol", "laugh"
    );

    private static final Set<String> SAD = Set.of(
            "sad", "terrible", "awful", "bad", "hate", "horrible", "death",
            "crash", "crisis", "war", "fail", "disaster", "pain", "cry",
            "😢", "😭", "😞", "💔", "👎", ":(", ":-(", "tragedy", "dark"
    );

    /**
     * Constructor to initialize the service with WSClient and API key.
     * @param ws Play WSClient for HTTP requests
     * @param config Config object (currently API key hardcoded for simplicity)
     */
    @Inject
    public SentimentService(WSClient ws, com.typesafe.config.Config config) {
        this.ws = ws;
        this.apiKey = "cf69ac0f4dd54ce4a2a5e00503ecaf77";
    }

    /**
     * Computes sentiment for a given query asynchronously.
     * Fetches up to 50 news articles and aggregates their sentiment.
     *
     * @param query Query string
     * @return CompletionStage of one of ":-)", ":-(", or ":-|"
     */
    public CompletionStage<String> sentimentForQuery(String query) {
        WSRequest req = ws.url("https://newsapi.org/v2/everything")
                .addQueryParameter("q", "\"" + query + "\"")
                .addQueryParameter("pageSize", "50")
                .addQueryParameter("sortBy", "publishedAt")
                .addQueryParameter("apiKey", apiKey);

        return req.get()
                .thenApply(r -> r.asJson())
                .thenApply(this::extractDescriptions)
                .thenApply(this::aggregateSentiment);
    }

    /* ---------- Helper: Extract article descriptions from JSON ---------- */
    private List<String> extractDescriptions(JsonNode root) {
        JsonNode arts = root.path("articles");
        if (!arts.isArray()) return List.of();

        return StreamSupport.stream(arts.spliterator(), false)
                .map(a -> a.path("description").asText("").toLowerCase())
                .filter(d -> !d.isBlank())
                .collect(Collectors.toList());
    }

    /* ---------- Helper: Aggregate sentiment from list of descriptions ---------- */
    private String aggregateSentiment(List<String> descriptions) {
        if (descriptions.isEmpty()) return ":-|";

        long happy = 0;
        long sad = 0;

        for (String desc : descriptions) {
            for (String token : desc.split("\\W+")) {
                if (HAPPY.contains(token)) happy++;
                if (SAD.contains(token)) sad++;
            }
        }

        long total = happy + sad;
        if (total == 0) return ":-|";

        double happyRatio = (double) happy / total;
        double sadRatio   = (double) sad   / total;

        if (happyRatio > 0.7) return ":-)";
        if (sadRatio   > 0.7) return ":-(";
        return ":-|";
    }

    /**
     * Synchronous stub method (for testing) returning query string directly.
     * @param query Input query
     * @return Same query string
     */
    public String sentimentForQuerySync(String query) {
        return query;
    }
}
