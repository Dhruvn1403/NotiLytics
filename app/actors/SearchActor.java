package actors;

import models.Article;
import services.NewsApiClient;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Actor responsible for performing repeated searches for a query
 * and sending results to a WebSocket session.
 * It maintains a cache of already sent articles to avoid duplicates.
 *
 * Sends two types of messages to {@link UserSessionActor}:
 * 1. "article_batch" - initial batch of articles
 * 2. "new_article" - incremental updates for newly discovered articles
 *
 * Polls the NewsApiClient every 10 seconds for updates.
 *
 * @author Manush Shah
 */
public class SearchActor extends AbstractBehavior<SearchActor.Command> {

    /** Marker interface for all commands this actor can handle */
    public interface Command {}

    /** Command to trigger a search for the given query */
    public record PerformSearch(String query) implements Command {}

    private final ActorRef<UserSessionActor.WsMessage> out;
    private final NewsApiClient apiClient;

    /** Keep a set of last seen URLs to deduplicate articles */
    private final LinkedHashSet<String> lastSeenUrls = new LinkedHashSet<>();

    /**
     * Factory method to create a SearchActor.
     *
     * @param out WebSocket actor to send results to
     * @param apiClient News API client for fetching articles
     * @return Behavior of SearchActor
     */
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

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(PerformSearch.class, this::onPerformSearch)
                .build();
    }

    /**
     * Handles the PerformSearch command.
     * Fetches latest articles from the API and sends fresh items to the WebSocket.
     *
     * @param msg PerformSearch command with the query
     * @return same actor behavior
     */
    private Behavior<Command> onPerformSearch(PerformSearch msg) {
        final String query = msg.query();

        // fetch up to 50 articles
        CompletionStage<List<Article>> future = apiClient.searchArticles(query, 50);

        future.whenComplete((articles, ex) -> {
            if (ex != null) {
                // Notify client of errors
                String err = "{\"type\":\"error\",\"message\":\"Search failed: " + ex.getMessage() + "\"}";
                out.tell(new UserSessionActor.WsMessage(err));
                return;
            }

            List<Article> sorted = articles.stream().collect(Collectors.toList());
            List<Article> fresh = new ArrayList<>();

            // Identify fresh articles not seen before
            for (Article a : sorted) {
                String url = a.getUrl() == null ? a.getTitle() : a.getUrl();
                if (!lastSeenUrls.contains(url)) {
                    fresh.add(a);
                }
            }

            if (lastSeenUrls.isEmpty()) {
                // Initial batch: send up to 10 articles
                List<Article> initial = sorted.stream().limit(10).collect(Collectors.toList());
                initial.stream()
                        .map(a -> a.getUrl() == null ? a.getTitle() : a.getUrl())
                        .forEach(this::addToCache);
                String json = buildBatchJson(initial, "article_batch");
                out.tell(new UserSessionActor.WsMessage(json));
            } else if (!fresh.isEmpty()) {
                // Send incremental updates
                for (Article a : fresh) {
                    addToCache(a.getUrl() == null ? a.getTitle() : a.getUrl());
                    String json = buildArticleJson(a, "new_article");
                    out.tell(new UserSessionActor.WsMessage(json));
                }
            }
        });

        // Schedule next poll in 10 seconds
        getContext().scheduleOnce(
                java.time.Duration.ofSeconds(10),
                getContext().getSelf(),
                new PerformSearch(query)
        );

        return this;
    }

    /** Add URL to cache, keeping the cache size bounded */
    private void addToCache(String url) {
        lastSeenUrls.add(url);
        if (lastSeenUrls.size() > 2000) {
            Iterator<String> it = lastSeenUrls.iterator();
            it.next();
            it.remove();
        }
    }

    /** Build JSON string for a batch of articles */
    private String buildBatchJson(List<Article> list, String type) {
        return play.libs.Json.toJson(Map.of(
                "type", type,
                "articles", list
        )).toString();
    }

    /** Build JSON string for a single article */
    private String buildArticleJson(Article a, String type) {
        return play.libs.Json.toJson(Map.of(
                "type", type,
                "article", a
        )).toString();
    }
}
