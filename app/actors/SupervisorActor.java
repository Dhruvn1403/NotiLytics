package actors;

import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/**
 * SupervisorActor that can supervise children with restart on failure.
 */
public class SupervisorActor {

    public static Behavior<Void> create() {
        // Explicitly specify <Void> type for supervise
        return Behaviors.<Void>supervise(Behaviors.<Void>empty())
                .onFailure(Exception.class, SupervisorStrategy.restart());
    }
}
