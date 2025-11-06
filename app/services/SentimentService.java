package services;

import models.Article;
import java.util.concurrent.CompletionStage;

/**
 * @author Jaiminkumar Mayani
 */

public interface SentimentService {
    /** Returns one of “:-)”, “:-(” or “:-|” for the given query. */
    CompletionStage<String> sentimentForQuery(String query);
}