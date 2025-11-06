package models;
import java.util.List;

/**
 * Immutable model describing a news source plus the most recent articles.
 * Using a Java record keeps it concise and generates accessors:
 *   id(), name(), description(), url(), category(), language(), country(), articles()
 */
public record SourceInfo(
        String id,
        String name,
        String description,
        String url,
        String category,
        String language,
        String country,
        List<Article> articles
) {}
