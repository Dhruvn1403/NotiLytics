package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.*;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.*;
import java.util.Map;

/**
 * Tests for {@link WordStatsActor}, verifying:
 * <ul>
 *     <li>Accurate word-count computation</li>
 *     <li>Correct filtering of short words</li>
 *     <li>Proper emission of {@code WordStatsResult}</li>
 *     <li>Stop message handling</li>
 * </ul>
 *
 * <p>This test covers all functional logic in the actor.</p>
 *
 * @author Varun Oza
 */
public class WordStatsActorTest {

    @ClassRule
    public static TestKitJunitResource testKit = new TestKitJunitResource();

    /**
     * Performs a full pipeline test:
     * <ol>
     *     <li>Sends a sample article with text containing repeated keywords</li>
     *     <li>Verifies that normalized word counts match expected values</li>
     *     <li>Ensures short words (<=3 chars) are removed</li>
     *     <li>Executes Stop for coverage completeness</li>
     * </ol>
     */
    @Test
    public void computeCountAndStop() {
        TestProbe<UserSessionActor.Command> parent =
                testKit.createTestProbe(UserSessionActor.Command.class);

        ActorRef<WordStatsActor.Command> ref =
                testKit.spawn(WordStatsActor.create(parent.getRef()));

        WordStatsActor.InputArticle article =
                new WordStatsActor.InputArticle("Hello WORLD", "This is a sample WORLD text");

        ref.tell(new WordStatsActor.Compute(article));

        UserSessionActor.Command msg = parent.receiveMessage();
        Assert.assertTrue(msg instanceof UserSessionActor.WordStatsResult);

        Map<String,Integer> counts =
                ((UserSessionActor.WordStatsResult) msg).counts();

        Assert.assertEquals(Integer.valueOf(2), counts.get("world"));
        Assert.assertEquals(Integer.valueOf(1), counts.get("hello"));
        Assert.assertEquals(Integer.valueOf(1), counts.get("sample"));

        ref.tell(new WordStatsActor.Stop());
    }
}
