package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import services.SentimentService;

import java.util.HashMap;
import java.util.Map;

public class SentimentActor extends AbstractBehavior<SentimentActor.Command> {

    public interface Command {}

    public static final class Analyze implements Command {
        public final String query;
        public Analyze(String query) { this.query = query; }
    }

    public static final class Stop implements Command {}

    private final ActorRef<UserSessionActor.Command> parent;
    private final SentimentService sentimentService;

    public static Behavior<Command> create(ActorRef<UserSessionActor.Command> parent, SentimentService sentimentService) {
        return Behaviors.setup(ctx -> new SentimentActor(ctx, parent, sentimentService));
    }

    private SentimentActor(ActorContext<Command> ctx, ActorRef<UserSessionActor.Command> parent, SentimentService sentimentService) {
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

    private Behavior<Command> onAnalyze(Analyze msg) {
        // Log call
        System.out.println("[SentimentActor] Analyzing query: " + msg.query);

        // Call sentiment service
        String sentiment = sentimentService.sentimentForQuerySync(msg.query); // sync version for simplicity

        // Wrap in JSON and send to session actor
        Map<String,Object> payload = new HashMap<>();
        payload.put("type", "sentiment");
        payload.put("query", msg.query);
        payload.put("sentiment", sentiment);

        parent.tell(new UserSessionActor.WsMessage(play.libs.Json.toJson(payload).toString()));

        return this;
    }

    private Behavior<Command> onStop() {
        return Behaviors.stopped();
    }
}
