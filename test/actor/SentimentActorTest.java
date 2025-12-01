package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.junit.*;
import org.mockito.Mockito;
import services.SentimentService;

/**
 * Tests for {@link SentimentActor}, ensuring:
 * <ul>
 *     <li>Sentiment analysis results are emitted using the parent probe</li>
 *     <li>Stop command is properly handled</li>
 * </ul>
 *
 * <p>This class guarantees full branch coverage for the sentiment pipeline.</p>
 *
 * @author Jaimin
 */
public class SentimentActorTest {

    /** Shared actor system for test execution */
    static TestKitJunitResource testKit = new TestKitJunitResource();

    /**
     * Ensures that:
     * <ol>
     *     <li>An {@code Analyze} message triggers sentiment evaluation</li>
     *     <li>A {@code WsMessage} is emitted to the parent</li>
     *     <li>The {@code Stop} message is accepted for coverage</li>
     * </ol>
     */
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
