// /**
//  * Tests for source profile + linking from search results.
//  * @author Java Developers
//  */
// package controllers;

// import models.Article;
// import models.SourceInfo;
// import org.junit.Test;
// import play.mvc.Result;
// import services.NewsApiClient;

// import java.util.List;
// import java.util.concurrent.CompletableFuture;

// import static org.junit.Assert.*;
// import static org.mockito.Mockito.*;
// import static play.test.Helpers.OK;
// import static play.test.Helpers.contentAsString;

// public class NotiControllerTest {
//     @Test
//     public void sourceProfile_renders() {
//         NewsApiClient api = mock(NewsApiClient.class);
//         NotiController controller = new NotiController(api);

//         SourceInfo info = new SourceInfo(
//                 "demo","Demo","Desc","https://demonews.com","business","en","us",
//                 List.of(new Article("T","https://x","Demo","https://demonews.com","2025-11-01","d",0.1))
//         );
//         when(api.sourceProfileByName("Demo")).thenReturn(CompletableFuture.completedFuture(info));

//         Result r = controller.sourceProfile("Demo").toCompletableFuture().join();
//         assertEquals(OK, r.status());
//         assertTrue(contentAsString(r).contains("Demo"));
//     }
// }
