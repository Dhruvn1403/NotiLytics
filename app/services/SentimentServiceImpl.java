package services;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Jaiminkumar Mayani
 */

@Singleton
public final class SentimentServiceImpl implements SentimentService {

    private final WSClient ws;
    private final String apiKey;

    /* ---------- happy / sad lists ---------- */
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
    public SentimentServiceImpl(WSClient ws, com.typesafe.config.Config config) {
        this.ws = ws;
//        this.apiKey = Optional.ofNullable(System.getenv("NEWSAPI_KEY"))
//                              .filter(k -> !k.isBlank())
//                              .orElse(config.getString("newsapi.key"));
        this.apiKey = "cf69ac0f4dd54ce4a2a5e00503ecaf77";
    }

    @Override
    public CompletionStage<String> sentimentForQuery(String query) {
        WSRequest req = ws.url("https://newsapi.org/v2/everything")
                .addQueryParameter("q", "\"" + query + "\"")   // phrase search
                .addQueryParameter("pageSize", "50")
                .addQueryParameter("sortBy", "publishedAt")
                .addQueryParameter("apiKey", apiKey);

        return req.get()
                .thenApply(r -> r.asJson())
                .thenApply(this::extractDescriptions)
                .thenApply(this::aggregateSentiment);
    }

    /* ---------- helpers ---------- */

    private List<String> extractDescriptions(JsonNode root) {
        JsonNode arts = root.path("articles");
        if (!arts.isArray()) return List.of();
        return StreamSupport.stream(arts.spliterator(), false)
                .map(a -> a.path("description").asText("").toLowerCase())
                .filter(d -> !d.isBlank())
                .collect(Collectors.toList());
    }

    String aggregateSentiment(List<String> descriptions) {
        if (descriptions.isEmpty()) return ":-|";

        long[] counters = descriptions.stream()
                .flatMapToLong(desc -> {
                    long[] c = {0, 0};          // [happy, sad]
                    String[] tokens = desc.split("\\W+");
                    for (String t : tokens) {
                        if (HAPPY.contains(t)) c[0]++;
                        if (SAD.contains(t))   c[1]++;
                    }
                    return Arrays.stream(c);
                })
                .toArray();

        long happy = counters[0];
        long sad   = counters[1];
        long total = happy + sad;
        if (total == 0) return ":-|";

        double happyRatio = (double) happy / total;
        double sadRatio   = (double) sad   / total;

        if (happyRatio > 0.7) return ":-)";
        if (sadRatio   > 0.7) return ":-(";
        return ":-|";
    }
}