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

public class SearchActorTest {

    static TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void initialBatchAndIncrementalAndError() {
        var probe = TestProbe.<UserSessionActor.WsMessage>create(testKit.system());
        NewsApiClient api = Mockito.mock(NewsApiClient.class);

        // Phase 1 initial -> returns two
        Assert.assertNotNull(api);
        Mockito.when(api.searchArticles("ai",50))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new Article("T1","u1","s","su","2024","d",1),
                        new Article("T2","u2","s","su","2024","d",1)
                )));

        var ref = testKit.spawn(SearchActor.create(probe.getRef(), api));

        ref.tell(new SearchActor.PerformSearch("ai"));
        probe.expectMessageClass(UserSessionActor.WsMessage.class);

        // Phase 2 incremental new article
        Mockito.when(api.searchArticles("ai",50))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new Article("T1","u1","s","su","2024","d",1),
                        new Article("TNEW","un","s","su","2024","d",1)
                )));

        ref.tell(new SearchActor.PerformSearch("ai"));
        probe.expectMessageClass(UserSessionActor.WsMessage.class); // incremental fires

        // Phase 3 error
        Mockito.when(api.searchArticles("ai",50))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("fail")));

        ref.tell(new SearchActor.PerformSearch("ai"));
        probe.expectMessageClass(UserSessionActor.WsMessage.class);
    }
}
