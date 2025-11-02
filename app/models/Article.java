package models;
import java.time.ZonedDateTime;
public record Article(
        String title, String url, String sourceName, String sourceUrl,
        String author, String description, ZonedDateTime publishedAtEt
) {}
