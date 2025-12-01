package actors;

import models.SourceInfo;
import services.NewsSources;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import java.util.List;

/**
 * Actor wrapper around NewsSources service (Monil Tailor).
 * Fetches news sources asynchronously and sends results to a WebSocket session.
 *
 * @author Monil Tailor
 */
public class SourcesActor extends AbstractBehavior<SourcesActor.Command> {

    /** Marker interface for all actor commands */
    public interface Command {}

    /** Command to fetch news sources with optional filters */
    public record Fetch(String country, String category, String lang) implements Command {}

    private final ActorRef<UserSessionActor.WsMessage> out;
    private final NewsSources newsSources;

    /** Factory method to create actor */
    public static Behavior<Command> create(
            ActorRef<UserSessionActor.WsMessage> out,
            NewsSources newsSources
    ) {
        return Behaviors.setup(ctx -> new SourcesActor(ctx, out, newsSources));
    }

    private SourcesActor(
            ActorContext<Command> ctx,
            ActorRef<UserSessionActor.WsMessage> out,
            NewsSources newsSources
    ) {
        super(ctx);
        this.out = out;
        this.newsSources = newsSources;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Fetch.class, this::onFetch)
                .build();
    }

    /**
     * Handles the Fetch command by calling NewsSources service asynchronously.
     * Sends the resulting list of SourceInfo objects to the WebSocket client.
     */
    private Behavior<Command> onFetch(Fetch msg) {
        newsSources.fetchSources(msg.country(), msg.category(), msg.lang())
                .thenAccept(list ->
                        out.tell(new UserSessionActor.WsMessage(
                                play.libs.Json.toJson(list).toString()
                        ))
                );
        return this;
    }
}
