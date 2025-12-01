package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import models.SourceInfo;
import org.junit.*;
import org.mockito.Mockito;
import services.NewsSources;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link SourcesActor} verifying correct handling of source-fetching
 * requests and ensuring results are sent back through WebSocket messages.
 *
 * <p>Only the success path is required for full coverage, since the failure path
 * is indirectly covered by {@link UserSessionActorTest}.</p>
 *
 * @author Monil
 */
public class SourcesActorTest {

    static TestKitJunitResource testKit = new TestKitJunitResource();

    /**
     * Validates that a successful call to {@link NewsSources#fetchSources}
     * leads to a {@code WsMessage} being emitted to the parent actor.
     */
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
