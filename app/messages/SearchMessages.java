package messages;

import models.Article;
import java.util.List;

public class SearchMessages {
    public record StartSearch(String query) {}
    public record NewArticles(List<Article> articles) {}
    public record PollForUpdates(String query) {}
}
