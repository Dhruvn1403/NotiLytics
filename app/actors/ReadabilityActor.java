package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import services.ReadabilityService;

import java.util.HashMap;
import java.util.Map;

public class ReadabilityActor extends AbstractBehavior<ReadabilityActor.Command> {

    public interface Command {}

    public static final class Analyze implements Command {
        public final String text;
        public Analyze(String text) { this.text = text; }
    }

    public static final class Stop implements Command {}

    private final ActorRef<UserSessionActor.Command> parent;
    private final ReadabilityService readabilityService;

    public static Behavior<Command> create(ActorRef<UserSessionActor.Command> parent, ReadabilityService readabilityService) {
        return Behaviors.setup(ctx -> new ReadabilityActor(ctx, parent, readabilityService));
    }

    private ReadabilityActor(ActorContext<Command> ctx, ActorRef<UserSessionActor.Command> parent, ReadabilityService readabilityService) {
        super(ctx);
        this.parent = parent;
        this.readabilityService = readabilityService;
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
        System.out.println("[ReadabilityActor] Calculating readability for text: " + msg.text);

        // Call readability service
        double score = readabilityService.calculateReadability(msg.text);

        // Wrap in JSON and send to session actor
        Map<String,Object> payload = new HashMap<>();
        payload.put("type", "readability");
        payload.put("score", score);
        payload.put("textSnippet", msg.text.length() > 50 ? msg.text.substring(0,50)+"..." : msg.text);

        parent.tell(new UserSessionActor.WsMessage(play.libs.Json.toJson(payload).toString()));

        return this;
    }

    private Behavior<Command> onStop() {
        return Behaviors.stopped();
    }
}
