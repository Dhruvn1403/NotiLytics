package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.junit.*;

public class SupervisorActorTest {

    static TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void buildActor_Covers100() {
        testKit.spawn(SupervisorActor.create());
    }
}
