package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import java.util.*;
import java.util.StringTokenizer;

public class WordStatsActor extends AbstractBehavior<WordStatsActor.Command> {

    public interface Command {}

    public static final class InputArticle {
        public final String title;
        public final String description;
        public InputArticle(String title, String description) {
            this.title = title; this.description = description;
        }
    }

    public static final class Compute implements Command {
        public final InputArticle article;
        public Compute(InputArticle article) { this.article = article; }
    }

    public static final class Stop implements Command {}

    private final ActorRef<UserSessionActor.Command> parent;

    public static Behavior<Command> create(ActorRef<UserSessionActor.Command> parent) {
        return Behaviors.setup(ctx -> new WordStatsActor(ctx, parent));
    }

    private WordStatsActor(ActorContext<Command> ctx, ActorRef<UserSessionActor.Command> parent) {
        super(ctx);
        this.parent = parent;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Compute.class, this::onCompute)
                .onMessage(Stop.class, s -> this.onStop())
                .build();
    }

    private Behavior<Command> onCompute(Compute msg) {
        String text = (msg.article.title == null ? "" : msg.article.title) + " " + (msg.article.description == null ? "" : msg.article.description);
        Map<String, Integer> counts = computeWordCounts(text);
        // convert to normal Map<String,Integer> and send to parent
        parent.tell(new UserSessionActor.WordStatsResult(counts));
        return this;
    }

    private Behavior<Command> onStop() {
        return Behaviors.stopped();
    }

    private Map<String,Integer> computeWordCounts(String text) {
        Map<String,Integer> map = new HashMap<>();
        if (text == null || text.isEmpty()) return map;
        String normalized = text.replaceAll("[^A-Za-z0-9 ]", " ").toLowerCase();
        StringTokenizer st = new StringTokenizer(normalized);
        while (st.hasMoreTokens()) {
            String w = st.nextToken().trim();
            if (w.length() <= 3) continue; // skip tiny words
            map.put(w, map.getOrDefault(w, 0) + 1);
        }
        return map;
    }
}
