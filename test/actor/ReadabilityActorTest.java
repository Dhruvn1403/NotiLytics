package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.*;
import services.ReadabilityService;

import java.util.Map;

public class ReadabilityActorTest {

    @ClassRule
    public static TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testAnalyzeAndStop() {
        // Use Pekko TestProbe to act as fake parent
        TestProbe<UserSessionActor.Command> parentProbe =
                testKit.createTestProbe(UserSessionActor.Command.class);

        ReadabilityService svc = new ReadabilityService();

        ActorRef<ReadabilityActor.Command> ref =
                testKit.spawn(ReadabilityActor.create(parentProbe.getRef(), svc));

        // Trigger readability calculation
        ref.tell(new ReadabilityActor.Analyze("This sentence is very easy."));

        // Expect a WsMessage from the actor
        UserSessionActor.Command msg = parentProbe.receiveMessage();
        String json = ((UserSessionActor.WsMessage) msg).json();
        Assert.assertTrue(json.contains("\"type\":\"readability\""));

        // Stop the actor → covers Stop handler
        ref.tell(new ReadabilityActor.Stop());
    }
}
