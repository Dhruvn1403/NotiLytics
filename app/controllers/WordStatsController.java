package controllers;

import models.WordStatsService;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletionStage;

/**
 * Async endpoint for Word Stats view.
 * GET /wordstats/:query
 *
 * Author: Your Name
 */
@Singleton
public class WordStatsController extends Controller {

    private final WordStatsService wordStatsService;

    @Inject
    public WordStatsController(WordStatsService wordStatsService) {
        this.wordStatsService = wordStatsService;
    }

    public CompletionStage<Result> showWordStats(Http.Request request, String query) {
    return wordStatsService.computeWordStats(query)
            .thenApply(stats ->
                    ok(views.html.wordStats.render(query, stats, request))
            );
}
}
