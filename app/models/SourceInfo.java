package models;
import java.util.List;
public record SourceInfo(
        String id, String name, String description, String url,
        String category, String language, String country,
        List<Article> latestArticles
) {}
