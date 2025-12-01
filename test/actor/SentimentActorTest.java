package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.junit.*;
import org.mockito.Mockito;
import services.SentimentService;

public class SentimentActorTest {

    static TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void analyzeAndStopCovered() {
        var parent = TestProbe.<UserSessionActor.Command>create(testKit.system());

        SentimentService mock = Mockito.mock(SentimentService.class);
        Mockito.when(mock.sentimentForQuerySync("ai")).thenReturn(":-)");

        var ref = testKit.spawn(SentimentActor.create(parent.getRef(), mock));

        ref.tell(new SentimentActor.Analyze("ai"));
        parent.expectMessageClass(UserSessionActor.WsMessage.class);

        ref.tell(new SentimentActor.Stop());
    }
}
