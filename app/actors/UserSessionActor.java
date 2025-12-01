package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;
import services.*;

import java.util.*;
import java.util.UUID;

public class UserSessionActor extends AbstractBehavior<UserSessionActor.Command> {

    public interface Command {}

    private final ActorRef<WsMessage> out;

    // Services
    private final NewsApiClient newsApiClient;
    private final NewsSources newsSources;
    private final SentimentService sentimentService;
    private final ReadabilityService readabilityService;

    // Child actors
    private final ActorRef<SearchActor.Command> searchActor;
    private final ActorRef<SourcesActor.Command> sourcesActor;
    private final ActorRef<WordStatsActor.Command> wordStatsActor;
    private final ActorRef<SentimentActor.Command> sentimentActor;
    private final ActorRef<ReadabilityActor.Command> readabilityActor;

    public static Behavior<Command> create(
            ActorRef<WsMessage> out,
            NewsApiClient newsApiClient,
            NewsSources newsSources,
            SentimentService sentimentService,
            ReadabilityService readabilityService
    ) {
        return Behaviors.setup(ctx -> new UserSessionActor(ctx, out, newsApiClient, newsSources, sentimentService, readabilityService));
    }

    private UserSessionActor(
            ActorContext<Command> ctx,
            ActorRef<WsMessage> out,
            NewsApiClient newsApiClient,
            NewsSources newsSources,
            SentimentService sentimentService,
            ReadabilityService readabilityService
    ) {
        super(ctx);
        this.out = out;
        this.newsApiClient = newsApiClient;
        this.newsSources = newsSources;
        this.sentimentService = sentimentService;
        this.readabilityService = readabilityService;

        // Spawn all child actors (simulate usage)
        this.searchActor = ctx.spawn(SearchActor.create(out, newsApiClient), "search-" + UUID.randomUUID());
        this.sourcesActor = ctx.spawn(SourcesActor.create(out, newsSources), "sources-" + UUID.randomUUID());
        this.wordStatsActor = ctx.spawn(WordStatsActor.create(ctx.getSelf()), "wordstats-" + UUID.randomUUID());
        this.sentimentActor = ctx.spawn(SentimentActor.create(ctx.getSelf(), sentimentService), "sentiment-" + UUID.randomUUID());
        this.readabilityActor = ctx.spawn(ReadabilityActor.create(ctx.getSelf(), readabilityService), "readability-" + UUID.randomUUID());
    }

    // Example commands from WebSocket
    public record StartSearch(String query) implements Command {}
    public record FetchSources(String country, String category, String lang) implements Command {}
    public record AnalyzeSentiment(String text) implements Command {}
    public record AnalyzeReadability(String text) implements Command {}
    public record WordStatsResult(Map<String,Integer> counts) implements Command {}

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartSearch.class, msg -> { searchActor.tell(new SearchActor.PerformSearch(msg.query())); return this; })
                .onMessage(FetchSources.class, msg -> { sourcesActor.tell(new SourcesActor.Fetch(msg.country(), msg.category(), msg.lang())); return this; })
                .onMessage(AnalyzeSentiment.class, msg -> { sentimentActor.tell(new SentimentActor.Analyze(msg.text)); return this; })
                .onMessage(AnalyzeReadability.class, msg -> { readabilityActor.tell(new ReadabilityActor.Analyze(msg.text)); return this; })
                .onMessage(WordStatsResult.class, msg -> {
                    // push JSON to WebSocket
                    Map<String,Object> payload = new HashMap<>();
                    payload.put("type", "wordStats");
                    payload.put("counts", msg.counts);
                    out.tell(new WsMessage(play.libs.Json.toJson(payload).toString()));
                    return this;
                })
                .build();
    }

    public record WsMessage(String json) implements Command {}
}
