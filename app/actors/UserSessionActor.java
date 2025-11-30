// package actors;

// import services.NewsApiClient;
// import services.NewsSources;

// import java.util.UUID;

// import org.apache.pekko.actor.typed.*;
// import org.apache.pekko.actor.typed.javadsl.*;

// public class UserSessionActor extends AbstractBehavior<UserSessionActor.Command> {

//     public interface Command {}

//     private final ActorRef<WsMessage> out;
//     private final NewsApiClient newsApiClient;
//     private final NewsSources newsSources;

//     private final ActorRef<SearchActor.Command> searchActor;
//     private final ActorRef<SourcesActor.Command> sourcesActor;

//     public static Behavior<Command> create(
//         ActorRef<WsMessage> out,
//         NewsApiClient newsApiClient,
//         NewsSources newsSources
//     ) {
//         return Behaviors.setup(ctx -> new UserSessionActor(ctx, out, newsApiClient, newsSources));
//     }

//     private UserSessionActor(
//             ActorContext<Command> ctx,
//             ActorRef<WsMessage> out,
//             NewsApiClient newsApiClient,
//             NewsSources newsSources
//     ) {
//         super(ctx);
//         this.out = out;
//         this.newsApiClient = newsApiClient;
//         this.newsSources = newsSources;

//         this.searchActor =
//                 ctx.spawn(SearchActor.create(out, newsApiClient), "search-" + UUID.randomUUID());

//         this.sourcesActor =
//                 ctx.spawn(SourcesActor.create(out, newsSources), "sources-" + UUID.randomUUID());
//     }

//     public record StartSearch(String query) implements Command {}
//     public record FetchSources(String country, String category, String lang) implements Command {}

//     @Override
//     public Receive<Command> createReceive() {
//         return newReceiveBuilder()
//                 .onMessage(StartSearch.class, this::onStartSearch)
//                 .onMessage(FetchSources.class, this::onFetchSources)
//                 .build();
//     }

//     private Behavior<Command> onStartSearch(StartSearch msg) {
//         searchActor.tell(new SearchActor.PerformSearch(msg.query()));
//         return this;
//     }

//     private Behavior<Command> onFetchSources(FetchSources msg) {
//         sourcesActor.tell(new SourcesActor.Fetch(msg.country(), msg.category(), msg.lang()));
//         return this;
//     }

//     public record WsMessage(String json) {}
// }


package actors;

import services.NewsApiClient;
import services.NewsSources;

import java.util.UUID;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import java.util.*;

public class UserSessionActor extends AbstractBehavior<UserSessionActor.Command> {

    public interface Command {}

    private final ActorRef<WsMessage> out;
    private final NewsApiClient newsApiClient;
    private final NewsSources newsSources;

    private final ActorRef<SearchActor.Command> searchActor;
    private final ActorRef<SourcesActor.Command> sourcesActor;

    // individual-task child
    private final ActorRef<WordStatsActor.Command> wordStatsActor;

    // session state
    private final List<String> searchHistory = new ArrayList<>();
    private final Set<String> seenUrls = new LinkedHashSet<>(); // dedupe globally for session

    public static Behavior<Command> create(
        ActorRef<WsMessage> out,
        NewsApiClient newsApiClient,
        NewsSources newsSources
    ) {
        return Behaviors.setup(ctx -> new UserSessionActor(ctx, out, newsApiClient, newsSources));
    }

    private UserSessionActor(
            ActorContext<Command> ctx,
            ActorRef<WsMessage> out,
            NewsApiClient newsApiClient,
            NewsSources newsSources
    ) {
        super(ctx);
        this.out = out;
        this.newsApiClient = newsApiClient;
        this.newsSources = newsSources;

        // Spawn SearchActor and SourcesActor per session (they will poll/send via out)
        this.searchActor =
                ctx.spawn(SearchActor.create(out, newsApiClient), "search-" + UUID.randomUUID());

        this.sourcesActor =
                ctx.spawn(SourcesActor.create(out, newsSources), "sources-" + UUID.randomUUID());

        // Spawn WordStatsActor (individual task)
        this.wordStatsActor =
                ctx.spawn(WordStatsActor.create(ctx.getSelf()), "wordstats-" + UUID.randomUUID());
    }

    // input commands from WebSocket (controller/classic-adapter will send these)
    public record StartSearch(String query) implements Command {}
    public record FetchSources(String country, String category, String lang) implements Command {}

    // internal message from word-stats child
    public record WordStatsResult(Map<String,Integer> counts) implements Command {}

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartSearch.class, this::onStartSearch)
                .onMessage(FetchSources.class, this::onFetchSources)
                .onMessage(WordStatsResult.class, this::onWordStatsResult)
                .build();
    }

    private Behavior<Command> onStartSearch(StartSearch msg) {
        String q = msg.query();
        // record search history
        searchHistory.add(0, q); // newest first
        if (searchHistory.size() > 20) searchHistory.remove(searchHistory.size() - 1);

        // tell search actor to perform the search and start polling
        searchActor.tell(new SearchActor.PerformSearch(q));
        return this;
    }

    private Behavior<Command> onFetchSources(FetchSources msg) {
        sourcesActor.tell(new SourcesActor.Fetch(msg.country(), msg.category(), msg.lang()));
        return this;
    }

    // called by WordStatsActor to deliver computed counts
    private Behavior<Command> onWordStatsResult(WordStatsResult msg) {
        // Wrap as JSON and send to client. Client expects JSON strings.
        Map<String,Object> payload = new HashMap<>();
        payload.put("type", "wordStats");
        payload.put("counts", msg.counts);
        out.tell(new WsMessage(play.libs.Json.toJson(payload).toString()));
        return this;
    }

    /* Utility exposed so children (SearchActor) can send processed articles to this session if needed.
       However SearchActor currently sends JSON directly to 'out'. To also forward each article to WordStats,
       you can modify SearchActor to also send UserSessionActor.NewArticle messages; for simplicity,
       we will show how to handle a NewArticle message if SearchActor was updated:
    */
    public record NewArticle(ArticleProxy article) implements Command {}

    // A simple lightweight article holder for internal actor messages (avoid dependency cycle)
    public static class ArticleProxy {
        public final String title;
        public final String url;
        public final String sourceName;
        public final String publishedAt;
        public final String description;

        public ArticleProxy(String title, String url, String sourceName, String publishedAt, String description) {
            this.title = title; this.url = url; this.sourceName = sourceName;
            this.publishedAt = publishedAt; this.description = description;
        }
    }

    private Behavior<Command> onNewArticle(NewArticle msg) {
        // dedupe at session level
        String key = (msg.article.url == null || msg.article.url.isBlank()) ? msg.article.title : msg.article.url;
        if (seenUrls.contains(key)) return this;
        seenUrls.add(key);
        if (seenUrls.size() > 5000) {
            Iterator<String> it = seenUrls.iterator();
            it.next();
            it.remove();
        }

        // push article JSON to client (most of this is done by SearchActor already)
        Map<String,Object> payload = new HashMap<>();
        payload.put("type", "new_article");
        payload.put("article", Map.of(
                "title", msg.article.title,
                "url", msg.article.url,
                "sourceName", msg.article.sourceName,
                "publishedAt", msg.article.publishedAt,
                "description", msg.article.description
        ));
        out.tell(new WsMessage(play.libs.Json.toJson(payload).toString()));

        // forward to WordStatsActor for analysis
        // create a simple Article object for wordstats child — we rely on WordStatsActor to accept minimal info
        WordStatsActor.InputArticle ia = new WordStatsActor.InputArticle(msg.article.title, msg.article.description);
        wordStatsActor.tell(new WordStatsActor.Compute(ia));

        return this;
    }

    public record WsMessage(String json) {}
}
