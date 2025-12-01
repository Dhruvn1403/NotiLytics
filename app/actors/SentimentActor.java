package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import services.SentimentService;

import java.util.HashMap;
import java.util.Map;

/**
 * Actor that analyzes the sentiment of a given query and sends
 * the results to a WebSocket session actor.
 *
 * Sends messages to {@link UserSessionActor} in the following JSON format:
 * {
 *     "type": "sentiment",
 *     "query": "<original query>",
 *     "sentiment": ":-| / :-) / :-("
 * }
 *
 * Polling or async repetition is not handled here; the parent actor
 * decides when to trigger sentiment analysis.
 *
 * @author Jaimin Mayani
 */
public class SentimentActor extends AbstractBehavior<SentimentActor.Command> {

    /** Marker interface for all commands this actor can handle */
    public interface Command {}

    /** Command to perform sentiment analysis for the given query */
    public static final class Analyze implements Command {
        public final String query;
        public Analyze(String query) { this.query = query; }
    }

    /** Command to stop this actor */
    public static final class Stop implements Command {}

    private final ActorRef<UserSessionActor.Command> parent;
    private final SentimentService sentimentService;

    /**
     * Factory method to create a SentimentActor.
     *
     * @param parent Actor to send results to (WebSocket session)
     * @param sentimentService Service to calculate sentiment
     * @return Behavior of SentimentActor
     */
    public static Behavior<Command> create(
            ActorRef<UserSessionActor.Command> parent,
            SentimentService sentimentService
    ) {
        return Behaviors.setup(ctx -> new SentimentActor(ctx, parent, sentimentService));
    }

    private SentimentActor(
            ActorContext<Command> ctx,
            ActorRef<UserSessionActor.Command> parent,
            SentimentService sentimentService
    ) {
        super(ctx);
        this.parent = parent;
        this.sentimentService = sentimentService;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Analyze.class, this::onAnalyze)
                .onMessage(Stop.class, s -> onStop())
                .build();
    }

    /**
     * Handles Analyze command: computes sentiment and sends JSON payload to parent actor.
     *
     * @param msg Analyze command
     * @return same actor behavior
     */
    private Behavior<Command> onAnalyze(Analyze msg) {
        System.out.println("[SentimentActor] Analyzing query: " + msg.query);

        // Use synchronous sentiment for simplicity
        String sentiment = sentimentService.sentimentForQuerySync(msg.query);

        Map<String,Object> payload = new HashMap<>();
        payload.put("type", "sentiment");
        payload.put("query", msg.query);
        payload.put("sentiment", sentiment);

        parent.tell(new UserSessionActor.WsMessage(play.libs.Json.toJson(payload).toString()));

        return this;
    }

    /** Stops the actor */
    private Behavior<Command> onStop() {
        return Behaviors.stopped();
    }
}
