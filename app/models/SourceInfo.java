package models;

import java.util.List;

/**
 * Immutable model representing metadata of a news source along with
 * its recent articles. Designed for profile view or detailed source display.
 *
 * <p>Fields:
 * <ul>
 *     <li>{@code id} - Unique identifier for the source (from NewsAPI or internal).</li>
 *     <li>{@code name} - Name of the source.</li>
 *     <li>{@code description} - Description or summary of the source.</li>
 *     <li>{@code url} - Homepage URL of the source.</li>
 *     <li>{@code category} - News category (e.g., general, technology, sports).</li>
 *     <li>{@code language} - Language code (e.g., en for English).</li>
 *     <li>{@code country} - Country code (e.g., us for United States).</li>
 *     <li>{@code articles} - List of recent {@link Article} objects from this source.</li>
 * </ul>
 * </p>
 *
 * Using a Java record keeps the class concise and provides automatically
 * generated accessors for each field.
 *
 * @author Manush Shah
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
