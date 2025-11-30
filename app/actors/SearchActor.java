// package actors;

// import models.Article;
// import services.NewsApiClient;

// import org.apache.pekko.actor.typed.*;
// import org.apache.pekko.actor.typed.javadsl.*;

// import java.util.*;

// public class SearchActor extends AbstractBehavior<SearchActor.Command> {

//     public interface Command {}

//     private final ActorRef<UserSessionActor.WsMessage> out;
//     private final NewsApiClient apiClient;

//     private List<Article> lastSeen = new ArrayList<>();

//     public static Behavior<Command> create(
//             ActorRef<UserSessionActor.WsMessage> out,
//             NewsApiClient apiClient
//     ) {
//         return Behaviors.setup(ctx -> new SearchActor(ctx, out, apiClient));
//     }

//     private SearchActor(
//             ActorContext<Command> ctx,
//             ActorRef<UserSessionActor.WsMessage> out,
//             NewsApiClient apiClient
//     ) {
//         super(ctx);
//         this.out = out;
//         this.apiClient = apiClient;
//     }

//     public record PerformSearch(String query) implements Command {}

//     @Override
//     public Receive<Command> createReceive() {
//         return newReceiveBuilder()
//                 .onMessage(PerformSearch.class, this::onPerformSearch)
//                 .build();
//     }

//     private Behavior<Command> onPerformSearch(PerformSearch msg) {
//         apiClient.searchArticles(msg.query(), 50).thenAccept(articles -> {

//             List<Article> fresh = new ArrayList<>();
//             for (Article a : articles) {
//                 if (!lastSeen.contains(a)) {
//                     fresh.add(a);
//                 }
//             }

//             if (!fresh.isEmpty()) {
//                 lastSeen = articles;
//                 out.tell(new UserSessionActor.WsMessage(toJson(fresh)));
//             }
//         });

//         getContext().scheduleOnce(
//                 java.time.Duration.ofSeconds(10),
//                 getContext().getSelf(),
//                 new PerformSearch(msg.query())
//         );

//         return this;
//     }

//     private String toJson(List<Article> list) {
//         return play.libs.Json.toJson(list).toString();
//     }
// }


package actors;

import models.Article;
import services.NewsApiClient;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class SearchActor extends AbstractBehavior<SearchActor.Command> {

    public interface Command {}

    private final ActorRef<UserSessionActor.WsMessage> out;
    private final NewsApiClient apiClient;

    // keep a set of last seen URLs so we can dedupe
    private final LinkedHashSet<String> lastSeenUrls = new LinkedHashSet<>();

    public static Behavior<Command> create(
            ActorRef<UserSessionActor.WsMessage> out,
            NewsApiClient apiClient
    ) {
        return Behaviors.setup(ctx -> new SearchActor(ctx, out, apiClient));
    }

    private SearchActor(
            ActorContext<Command> ctx,
            ActorRef<UserSessionActor.WsMessage> out,
            NewsApiClient apiClient
    ) {
        super(ctx);
        this.out = out;
        this.apiClient = apiClient;
    }

    public record PerformSearch(String query) implements Command {}

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(PerformSearch.class, this::onPerformSearch)
                .build();
    }

    private Behavior<Command> onPerformSearch(PerformSearch msg) {
        final String query = msg.query();

        // fetch latest 10 (or limit 50 and trim later if you like)
        CompletionStage<List<Article>> future = apiClient.searchArticles(query, 50);

        // when we receive the articles
        future.whenComplete((articles, ex) -> {
            if (ex != null) {
                // notify client of error (simple message)
                String err = "{\"type\":\"error\",\"message\":\"Search failed: " + ex.getMessage() + "\"}";
                out.tell(new UserSessionActor.WsMessage(err));
                return;
            }

            // sort by publishedAt if needed (your API returns already sorted by publishedAt usually)
            List<Article> sorted = articles.stream().collect(Collectors.toList());

            // Build list of fresh articles (not seen before)
            List<Article> fresh = new ArrayList<>();
            for (Article a : sorted) {
                String url = a.getUrl() == null ? a.getTitle() : a.getUrl();
                if (!lastSeenUrls.contains(url)) {
                    fresh.add(a);
                }
            }

            // If this is the initial query (lastSeenUrls empty), send up to 10 latest items
            if (lastSeenUrls.isEmpty()) {
                List<Article> initial = sorted.stream().limit(10).collect(Collectors.toList());
                // Update lastSeenUrls with the initial set
                initial.stream()
                        .map(a -> a.getUrl() == null ? a.getTitle() : a.getUrl())
                        .forEach(u -> {
                            lastSeenUrls.add(u);
                            // keep bounded
                            if (lastSeenUrls.size() > 2000) {
                                Iterator<String> it = lastSeenUrls.iterator();
                                it.next();
                                it.remove();
                            }
                        });
                String json = buildBatchJson(initial, "article_batch");
                out.tell(new UserSessionActor.WsMessage(json));
            } else if (!fresh.isEmpty()) {
                // For incremental updates, send each new article individually (append)
                for (Article a : fresh) {
                    String url = a.getUrl() == null ? a.getTitle() : a.getUrl();
                    lastSeenUrls.add(url);
                    if (lastSeenUrls.size() > 2000) {
                        Iterator<String> it = lastSeenUrls.iterator();
                        it.next();
                        it.remove();
                    }
                    String json = buildArticleJson(a, "new_article");
                    out.tell(new UserSessionActor.WsMessage(json));
                }
            } // otherwise no new items: do nothing

        });

        // schedule next poll for this same query
        getContext().scheduleOnce(
                java.time.Duration.ofSeconds(10),
                getContext().getSelf(),
                new PerformSearch(query)
        );

        return this;
    }

    private String buildBatchJson(List<Article> list, String type) {
        // Build a compact JSON: { type: "article_batch", articles: [ ... ] }
        return play.libs.Json.toJson(Map.of(
                "type", type,
                "articles", list
        )).toString();
    }

    private String buildArticleJson(Article a, String type) {
        return play.libs.Json.toJson(Map.of(
                "type", type,
                "article", a
        )).toString();
    }
}
