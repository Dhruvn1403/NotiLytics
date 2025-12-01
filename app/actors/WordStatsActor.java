package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import java.util.*;

/**
 * Computes simple word-frequency statistics for an article’s
 * title + description and returns the result to the parent WebSocket
 * session actor.
 *
 * Filters out words of length <= 3 and normalizes text.
 *
 * @author Varun Oza
 */
public class WordStatsActor extends AbstractBehavior<WordStatsActor.Command> {

    /** Marker interface for all commands */
    public interface Command {}

    /** Incoming article wrapper */
    public static final class InputArticle {
        public final String title;
        public final String description;

        public InputArticle(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    /** Command to request word stats computation */
    public static final class Compute implements Command {
        public final InputArticle article;
        public Compute(InputArticle article) { this.article = article; }
    }

    /** Command to request this actor to stop */
    public static final class Stop implements Command {}

    private final ActorRef<UserSessionActor.Command> parent;

    /** Factory method */
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
                .onMessage(Stop.class, m -> onStop())
                .build();
    }

    /** Handles Compute(article) */
    private Behavior<Command> onCompute(Compute msg) {
        String text = ((msg.article.title == null ? "" : msg.article.title) + " " +
                (msg.article.description == null ? "" : msg.article.description));

        Map<String, Integer> counts = computeWordCounts(text);

        // Send result to session actor
        parent.tell(new UserSessionActor.WordStatsResult(counts));

        return this;
    }

    /** Stop this actor */
    private Behavior<Command> onStop() {
        return Behaviors.stopped();
    }

    /** Word frequency logic */
    private Map<String,Integer> computeWordCounts(String text) {
        Map<String,Integer> map = new HashMap<>();

        if (text == null || text.isEmpty())
            return map;

        // normalize
        String normalized = text.replaceAll("[^A-Za-z0-9 ]", " ").toLowerCase(Locale.ROOT);

        for (String word : normalized.split("\\s+")) {
            if (word.length() <= 3) continue;
            map.put(word, map.getOrDefault(word, 0) + 1);
        }

        return map;
    }
}
