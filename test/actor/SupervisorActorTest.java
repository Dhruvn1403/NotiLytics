package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.*;

/**
 * Smoke test for {@link SupervisorActor}.
 *
 * <p>The sole purpose of this test is to trigger actor creation so that
 * its initialization code is fully covered. No messaging is required since
 * {@link SupervisorActor} primarily serves as the application root supervisor.</p>
 *
 * @author Monil
 */
public class SupervisorActorTest {

    static TestKitJunitResource testKit = new TestKitJunitResource();

    /**
     * Ensures actor creation succeeds without errors,
     * satisfying coverage requirements for root actor startup.
     */
    @Test
    public void buildActor_Covers100() {
        testKit.spawn(SupervisorActor.create());
    }
}
