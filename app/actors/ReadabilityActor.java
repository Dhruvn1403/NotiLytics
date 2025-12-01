package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import services.ReadabilityService;

import java.util.HashMap;
import java.util.Map;

/**
 * Actor responsible for calculating the readability score of a text snippet.
 * Receives text from the session actor, computes readability using {@link ReadabilityService},
 * and sends the result back to the parent {@link UserSessionActor} as a JSON message.
 *
 * @author Dhruv Patel
 */
public class ReadabilityActor extends AbstractBehavior<ReadabilityActor.Command> {

    /** Marker interface for all commands this actor can receive */
    public interface Command {}

    /** Command to analyze the readability of a given text */
    public static final class Analyze implements Command {
        public final String text;

        public Analyze(String text) {
            this.text = text;
        }
    }

    /** Command to stop the actor */
    public static final class Stop implements Command {}

    private final ActorRef<UserSessionActor.Command> parent;
    private final ReadabilityService readabilityService;

    /**
     * Factory method to create a ReadabilityActor behavior.
     *
     * @param parent parent actor to receive results
     * @param readabilityService service used to compute readability
     * @return Behavior instance
     */
    public static Behavior<Command> create(ActorRef<UserSessionActor.Command> parent,
                                           ReadabilityService readabilityService) {
        return Behaviors.setup(ctx -> new ReadabilityActor(ctx, parent, readabilityService));
    }

    private ReadabilityActor(ActorContext<Command> ctx,
                             ActorRef<UserSessionActor.Command> parent,
                             ReadabilityService readabilityService) {
        super(ctx);
        this.parent = parent;
        this.readabilityService = readabilityService;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Analyze.class, this::onAnalyze)
                .onMessage(Stop.class, msg -> onStop())
                .build();
    }

    /**
     * Handles the Analyze command: computes readability and sends results to parent.
     *
     * @param msg Analyze command containing the text
     * @return same actor behavior
     */
    private Behavior<Command> onAnalyze(Analyze msg) {
        System.out.println("[ReadabilityActor] Calculating readability for text: " + msg.text);

        double score = readabilityService.calculateReadability(msg.text);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "readability");
        payload.put("score", score);
        payload.put("textSnippet", msg.text.length() > 50 ? msg.text.substring(0, 50) + "..." : msg.text);

        parent.tell(new UserSessionActor.WsMessage(play.libs.Json.toJson(payload).toString()));

        return this;
    }

    /**
     * Handles the Stop command: stops this actor.
     *
     * @return Behavior that stops the actor
     */
    private Behavior<Command> onStop() {
        return Behaviors.stopped();
    }
}
