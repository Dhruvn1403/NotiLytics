package controllers;

import models.SourceInfo;
import play.mvc.Controller;
import play.mvc.Result;
import services.NewsApiClient;
import views.html.source;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

/**
 * Controller for source profiles and source browsing.
 * Shows profile info of a source website and its last 10 articles.
 * @author Manush Shah
 */
public class NotiController extends Controller {

    private final NewsApiClient news;

    @Inject
    public NotiController(NewsApiClient news) {
        this.news = news;
    }

    /** Non-blocking Source Profile UI */
    public CompletionStage<Result> sourceProfile(String name) {
        return news.sourceProfileByName(name)
                .thenApply((SourceInfo info) -> ok(source.render(info)));
    }
}
