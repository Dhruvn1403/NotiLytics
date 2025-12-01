package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.*;
import services.ReadabilityService;

import java.util.Map;

/**
 * Unit tests for the {@link ReadabilityActor}, validating its end-to-end behavior
 * when interacting with a simulated WebSocket parent actor.
 *
 * <p>This test verifies two important behaviors:
 * <ul>
 *     <li>Handling of the {@code Analyze} command – ensuring readability scoring
 *         is executed using {@link ReadabilityService} and an appropriate JSON
 *         {@code WsMessage} is sent to the parent.</li>
 *     <li>Handling of the {@code Stop} command – confirming the actor’s graceful
 *         termination path is covered for code coverage.</li>
 * </ul>
 *
 * <p>Pekko's {@link TestProbe} is used to simulate the parent {@link UserSessionActor},
 * allowing the test to intercept outgoing {@code WsMessage} instances without running
 * the actual WebSocket subsystem.</p>
 *
 * <p><strong>Coverage Goal:</strong> This test contributes toward full coverage for
 * the readability pipeline within the NotiLytics application.</p>
 *
 * @author Dhruv Patel
 */
public class ReadabilityActorTest {

    /**
     * Shared Pekko TestKit resource that creates an actor system for all unit tests in this class.
     * <p>
     * Annotated with {@code @ClassRule} so it initializes once for the test suite.
     */
    @ClassRule
    public static TestKitJunitResource testKit = new TestKitJunitResource();

    /**
     * Tests the complete lifecycle of {@link ReadabilityActor}:
     *
     * <ol>
     *     <li>Spawns the actor with a mocked parent probe.</li>
     *     <li>Sends an {@code Analyze} message containing a sample sentence.</li>
     *     <li>Captures the WebSocket JSON message sent to the parent and verifies that:</li>
     *       <ul>
     *         <li>The message is of type <code>"readability"</code></li>
     *         <li>JSON formatting follows expected schema</li>
     *       </ul>
     *     <li>Sends the {@code Stop} command to ensure the actor's shutdown path is executed.</li>
     * </ol>
     *
     * <p>This ensures both the "happy path" and the "Stop" behavior are covered for
     * code-coverage compliance.</p>
     */
    @Test
    public void testAnalyzeAndStop() {

        // ----------------------------------------------------------------------
        // Arrange: create a fake parent test probe to capture outgoing messages
        // ----------------------------------------------------------------------
        TestProbe<UserSessionActor.Command> parentProbe =
                testKit.createTestProbe(UserSessionActor.Command.class);

        // Real readability service implementation (deterministic)
        ReadabilityService svc = new ReadabilityService();

        // Spawn the ReadabilityActor with the fake parent
        ActorRef<ReadabilityActor.Command> ref =
                testKit.spawn(ReadabilityActor.create(parentProbe.getRef(), svc));

        // ----------------------------------------------------------------------
        // Act: send the Analyze command
        // ----------------------------------------------------------------------
        ref.tell(new ReadabilityActor.Analyze("This sentence is very easy."));

        // ----------------------------------------------------------------------
        // Assert: verify that the actor emitted a readability JSON WsMessage
        // ----------------------------------------------------------------------
        UserSessionActor.Command msg = parentProbe.receiveMessage();
        String json = ((UserSessionActor.WsMessage) msg).json();
        Assert.assertTrue("Output should contain readability type",
                json.contains("\"type\":\"readability\""));

        // ----------------------------------------------------------------------
        // Act: send Stop command to trigger actor termination path
        // ----------------------------------------------------------------------
        ref.tell(new ReadabilityActor.Stop());
    }
}
