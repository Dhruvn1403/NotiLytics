package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import models.Article;
import org.junit.*;
import org.mockito.Mockito;
import services.NewsApiClient;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Unit tests for {@link SearchActor}, validating:
 * <ul>
 *     <li>Initial search result emissions</li>
 *     <li>Incremental new-article detection</li>
 *     <li>Error-handling fallback behavior</li>
 * </ul>
 *
 * <p>The {@link TestProbe} simulates the WebSocket-parent actor and receives
 * outgoing {@code WsMessage} objects. Mockito is used to fully control the
 * behavior of {@link NewsApiClient}, allowing deterministic responses for all
 * phases of the test.</p>
 *
 * @author Manush Shah
 */
public class SearchActorTest {

    /** Shared Pekko test environment */
    static TestKitJunitResource testKit = new TestKitJunitResource();

    /**
     * Tests the full lifecycle of search behavior:
     *
     * <ol>
     *    <li>Initial search returns two articles → emits a batch result.</li>
     *    <li>Second search returns a new article → incremental update emitted.</li>
     *    <li>Third search throws an exception → error is handled and still produces a WsMessage.</li>
     * </ol>
     *
     * <p>This ensures all branches in {@link SearchActor} are fully covered.</p>
     */
    @Test
    public void initialBatchAndIncrementalAndError() {
        var probe = TestProbe.<UserSessionActor.WsMessage>create(testKit.system());
        NewsApiClient api = Mockito.mock(NewsApiClient.class);

        // --- Phase 1: Initial search returns two articles ---
        Mockito.when(api.searchArticles("ai", 50))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new Article("T1","u1","s","su","2024","d",1),
                        new Article("T2","u2","s","su","2024","d",1)
                )));

        var ref = testKit.spawn(SearchActor.create(probe.getRef(), api));

        ref.tell(new SearchActor.PerformSearch("ai"));
        probe.expectMessageClass(UserSessionActor.WsMessage.class);

        // --- Phase 2: Incremental update with new article ---
        Mockito.when(api.searchArticles("ai",50))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new Article("T1","u1","s","su","2024","d",1),
                        new Article("TNEW","un","s","su","2024","d",1)
                )));

        ref.tell(new SearchActor.PerformSearch("ai"));
        probe.expectMessageClass(UserSessionActor.WsMessage.class);

        // --- Phase 3: Error case ---
        Mockito.when(api.searchArticles("ai",50))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("fail")));

        ref.tell(new SearchActor.PerformSearch("ai"));
        probe.expectMessageClass(UserSessionActor.WsMessage.class);
    }
}
