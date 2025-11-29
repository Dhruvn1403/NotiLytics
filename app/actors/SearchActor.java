package actors;

import models.Article;
import services.NewsApiClient;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import java.util.*;

public class SearchActor extends AbstractBehavior<SearchActor.Command> {

    public interface Command {}

    private final ActorRef<UserSessionActor.WsMessage> out;
    private final NewsApiClient apiClient;

    private List<Article> lastSeen = new ArrayList<>();

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
        apiClient.searchArticles(msg.query(), 50).thenAccept(articles -> {

            List<Article> fresh = new ArrayList<>();
            for (Article a : articles) {
                if (!lastSeen.contains(a)) {
                    fresh.add(a);
                }
            }

            if (!fresh.isEmpty()) {
                lastSeen = articles;
                out.tell(new UserSessionActor.WsMessage(toJson(fresh)));
            }
        });

        getContext().scheduleOnce(
                java.time.Duration.ofSeconds(10),
                getContext().getSelf(),
                new PerformSearch(msg.query())
        );

        return this;
    }

    private String toJson(List<Article> list) {
        return play.libs.Json.toJson(list).toString();
    }
}
