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
 * Single merged SentimentService class.
 * No interface, no separate implementation file.
 *
 * @author Jaiminkumar Mayani
 */
@Singleton
public final class SentimentService {

    private final WSClient ws;
    private final String apiKey;

    /* ---------- happy / sad word sets ---------- */
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

    @Inject
    public SentimentService(WSClient ws, com.typesafe.config.Config config) {
        this.ws = ws;
        // Load API key (you can improve this later)
        this.apiKey = "cf69ac0f4dd54ce4a2a5e00503ecaf77";
    }

    /**
     * Returns one of ":-)", ":-(" or ":-|" for the given query.
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

    /* ---------- Helper: Extract article descriptions ---------- */
    private List<String> extractDescriptions(JsonNode root) {
        JsonNode arts = root.path("articles");
        if (!arts.isArray()) return List.of();

        return StreamSupport.stream(arts.spliterator(), false)
                .map(a -> a.path("description").asText("").toLowerCase())
                .filter(d -> !d.isBlank())
                .collect(Collectors.toList());
    }

    /* ---------- Helper: Aggregate sentiment ---------- */
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
}
