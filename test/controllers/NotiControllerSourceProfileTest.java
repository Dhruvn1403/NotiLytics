package controllers;

import models.Article;
import models.SourceInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import play.mvc.Result;
import services.NewsApiClient;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static play.mvc.Http.Status.OK;

/** Unit test for source profile controller using a mocked service. */
public class NotiControllerSourceProfileTest {

    @Test
    public void sourceProfile_rendersPage() throws Exception {
        // mock service result
        NewsApiClient client = Mockito.mock(NewsApiClient.class);
        var art = new Article(
                "Hello Title",
                "https://example.com/a1",
                "ABC News",
                "https://example.com",
                "Author",
                "Desc",
                ZonedDateTime.now());
        var info = new SourceInfo("abc-news", "ABC News",
                "Demo profile", "https://abcnews.go.com",
                "general", "en", "us", List.of(art));

        Mockito.when(client.sourceProfileByName("ABC News"))
                .thenReturn(CompletableFuture.completedFuture(info));

        NotiController ctrl = new NotiController(client);
        Result res = ctrl.sourceProfile("ABC News").toCompletableFuture().get();

        assertEquals(OK, res.status());
        // Body check can be added with play-test Helpers if needed, but status is sufficient here.
    }
}
