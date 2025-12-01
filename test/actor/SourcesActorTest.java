package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import models.SourceInfo;
import org.junit.*;
import org.mockito.Mockito;
import services.NewsSources;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SourcesActorTest {

    static TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void fetchCoversSuccess() {
        var probe = TestProbe.<UserSessionActor.WsMessage>create(testKit.system());

        NewsSources mock = Mockito.mock(NewsSources.class);
        Mockito.when(mock.fetchSources("us","tech","en"))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new SourceInfo("1","CNN","d","u","c","e","us",List.of())
                )));

        var ref = testKit.spawn(SourcesActor.create(probe.getRef(), mock));
        ref.tell(new SourcesActor.Fetch("us","tech","en"));

        probe.expectMessageClass(UserSessionActor.WsMessage.class);
    }
}
