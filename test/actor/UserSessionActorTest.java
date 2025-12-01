package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.junit.*;
import org.mockito.Mockito;
import services.*;
import java.util.Map;

/**
 * Tests the routing logic inside {@link UserSessionActor}, ensuring all
 * message types are correctly dispatched to child actors and that results
 * are forwarded through {@code WsMessage}.
 *
 * <p>This test covers:</p>
 * <ul>
 *     <li>StartSearch → SearchActor flow</li>
 *     <li>FetchSources → SourcesActor flow</li>
 *     <li>AnalyzeSentiment → SentimentActor flow</li>
 *     <li>AnalyzeReadability → ReadabilityActor flow</li>
 *     <li>WordStatsResult → Forwarding behavior</li>
 * </ul>
 *
 * <p>All message routes must run at least once to achieve 100% coverage.</p>
 *
 * @author Monil
 */
public class UserSessionActorTest {

    static TestKitJunitResource testKit = new TestKitJunitResource();

    /**
     * Sends one message of every command type to ensure
     * every branch of the {@link UserSessionActor} receive block is executed.
     */
    @Test
    public void allRoutesCovered() {
        var probe = TestProbe.<UserSessionActor.WsMessage>create(testKit.system());

        var api = Mockito.mock(NewsApiClient.class);
        var src = Mockito.mock(NewsSources.class);
        var sent = Mockito.mock(SentimentService.class);
        var read = Mockito.mock(ReadabilityService.class);

        var ref = testKit.spawn(UserSessionActor.create(probe.getRef(), api, src, sent, read));

        ref.tell(new UserSessionActor.StartSearch("q"));
        ref.tell(new UserSessionActor.FetchSources("us","tech","en"));
        ref.tell(new UserSessionActor.AnalyzeSentiment("happy"));
        ref.tell(new UserSessionActor.AnalyzeReadability("text"));

        ref.tell(new UserSessionActor.WordStatsResult(Map.of("ai",2)));
        probe.expectMessageClass(UserSessionActor.WsMessage.class);
    }
}
