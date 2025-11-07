package models;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

//    @author Dhruv Patel
public class Article {
    public String title;
    public String url;
    public String sourceName;
    public String sourceUrl;
    public String publishedAt;
    public String description;
    public double readabilityScore;

    //    @author Dhruv Patel
    public static String convertToEDT(LocalDateTime time) {
        ZonedDateTime edt = time.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("America/Toronto"));
        return edt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    }

    //    @author Dhruv Patel
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

    //    @author Dhruv Patel
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public String getPublishedAt() { return publishedAt; }
    public double getReadabilityScore() { return readabilityScore; }
    public String getDescription() { return description; }
    public void setReadabilityScore(double readabilityScore) {
        this.readabilityScore = readabilityScore;
    }
}
