package actors;

import services.NewsApiClient;
import services.NewsSources;

import java.util.UUID;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

public class UserSessionActor extends AbstractBehavior<UserSessionActor.Command> {

    public interface Command {}

    private final ActorRef<WsMessage> out;
    private final NewsApiClient newsApiClient;
    private final NewsSources newsSources;

    private final ActorRef<SearchActor.Command> searchActor;
    private final ActorRef<SourcesActor.Command> sourcesActor;

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

        this.searchActor =
                ctx.spawn(SearchActor.create(out, newsApiClient), "search-" + UUID.randomUUID());

        this.sourcesActor =
                ctx.spawn(SourcesActor.create(out, newsSources), "sources-" + UUID.randomUUID());
    }

    public record StartSearch(String query) implements Command {}
    public record FetchSources(String country, String category, String lang) implements Command {}

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartSearch.class, this::onStartSearch)
                .onMessage(FetchSources.class, this::onFetchSources)
                .build();
    }

    private Behavior<Command> onStartSearch(StartSearch msg) {
        searchActor.tell(new SearchActor.PerformSearch(msg.query()));
        return this;
    }

    private Behavior<Command> onFetchSources(FetchSources msg) {
        sourcesActor.tell(new SourcesActor.Fetch(msg.country(), msg.category(), msg.lang()));
        return this;
    }

    public record WsMessage(String json) {}
}
