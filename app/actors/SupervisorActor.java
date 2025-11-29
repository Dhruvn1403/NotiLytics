package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

public class SupervisorActor {

    public static Behavior<Void> create() {
        return Behaviors.supervise(Behaviors.<Void>empty())
                .onFailure(Exception.class, SupervisorStrategy.restart());
    }
}
