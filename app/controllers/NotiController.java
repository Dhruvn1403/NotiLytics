package controllers;

import play.mvc.*;
import services.NewsApiClient;
import models.SourceInfo;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class NotiController extends Controller {

    private final NewsApiClient news;

    @Inject
    public NotiController(NewsApiClient news) {
        this.news = news;
    }

    public CompletionStage<Result> sourceProfile(String name) {
        return news.sourceProfileByName(name)
                .thenApply(info -> ok(views.html.source.render(info)));
    }
}
