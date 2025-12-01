package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.*;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.*;
import java.util.Map;

public class WordStatsActorTest {

    @ClassRule
    public static TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void computeCountAndStop() {
        // Parent probe
        TestProbe<UserSessionActor.Command> parent =
                testKit.createTestProbe(UserSessionActor.Command.class);

        // Spawn actor
        ActorRef<WordStatsActor.Command> ref =
                testKit.spawn(WordStatsActor.create(parent.getRef()));

        // Create test input
        WordStatsActor.InputArticle article =
                new WordStatsActor.InputArticle("Hello WORLD", "This is a sample WORLD text");

        // Send compute
        ref.tell(new WordStatsActor.Compute(article));

        // Receive message from WordStatsActor
        UserSessionActor.Command msg = parent.receiveMessage();
        Assert.assertTrue(msg instanceof UserSessionActor.WordStatsResult);

        Map<String,Integer> counts =
                ((UserSessionActor.WordStatsResult) msg).counts();

        // WORLD appears twice
        Assert.assertEquals(Integer.valueOf(2), counts.get("world"));

        // Hello → 1
        Assert.assertEquals(Integer.valueOf(1), counts.get("hello"));

        // sample → 1
        Assert.assertEquals(Integer.valueOf(1), counts.get("sample"));

        // words <= 3 chars removed: "this", "is", "a" → ignored

        // Stop actor (coverage)
        ref.tell(new WordStatsActor.Stop());
    }
}
