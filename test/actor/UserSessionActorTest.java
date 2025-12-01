package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.junit.*;
import org.mockito.Mockito;
import services.*;
import java.util.Map;

public class UserSessionActorTest {

    static TestKitJunitResource testKit = new TestKitJunitResource();

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
