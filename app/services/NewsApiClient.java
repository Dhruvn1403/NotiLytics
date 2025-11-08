/**
 * Fetch detailed profile of a source and its recent articles.
 * @return profile with last 10 articles
 * @author Manush Shah
 */
package services;

import models.SourceInfo;
import models.Article;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface NewsApiClient {
    CompletionStage<SourceInfo> sourceProfileByName(String sourceName);
    CompletionStage<List<Article>> searchArticles(String query, int limit);

}
