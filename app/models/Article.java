package models;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a news article with metadata including title, URL, source, publication date,
 * description, and readability score.
 * <p>
 * This class also provides a utility to convert UTC publication time to Eastern Daylight Time (EDT).
 * </p>
 *
 * @author Dhruv Patel
 */
public class Article {

    private String title;
    private String url;
    private String sourceName;
    private String sourceUrl;
    private String publishedAt;
    private String description;
    private double readabilityScore;

    /**
     * Converts a UTC LocalDateTime to Eastern Daylight Time (EDT) string.
     *
     * @param time the UTC time to convert
     * @return formatted string in "yyyy-MM-dd HH:mm:ss z" pattern
     * @author Dhruv Patel
     */
    public static String convertToEDT(LocalDateTime time) {
        ZonedDateTime edt = time.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("America/Toronto"));
        return edt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    }

    /**
     * Constructs a new Article instance.
     *
     * @param title            the title of the article
     * @param url              the URL of the article
     * @param sourceName       the name of the source
     * @param sourceUrl        the URL of the source
     * @param publishedAt      the publication date/time in EDT
     * @param description      the article description or content snippet
     * @param readabilityScore the calculated readability score for the article
     * @author Dhruv Patel
     */
    public Article(String title, String url, String sourceName, String sourceUrl,
                   String publishedAt, String description, double readabilityScore) {
        this.title = title;
        this.url = url;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.publishedAt = publishedAt;
        this.description = description;
        this.readabilityScore = readabilityScore;
    }

    // ---------------- Getters ----------------

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public String getPublishedAt() { return publishedAt; }
    public String getDescription() { return description; }
    public double getReadabilityScore() { return readabilityScore; }

    // ---------------- Setters ----------------

    /**
     * Sets the readability score for this article.
     *
     * @param readabilityScore the new readability score
     * @author Dhruv Patel
     */
    public void setReadabilityScore(double readabilityScore) {
        this.readabilityScore = readabilityScore;
    }
}
