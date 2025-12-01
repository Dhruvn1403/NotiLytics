package messages;

import models.SourceInfo;
import models.Article;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SourceMessagesTest {

    @Test
    public void outerClass_isLoadable() {
        // Explicitly instantiate – required for Jacoco coverage of outer class
        new SourceMessages();
    }

    @Test
    public void fetchSources_record_works() {
        SourceMessages.FetchSources msg =
                new SourceMessages.FetchSources("us", "business", "en");

        assertEquals("us", msg.country());
        assertEquals("business", msg.category());
        assertEquals("en", msg.language());
    }

    @Test
    public void sourcesResult_record_works() {
        Article a = new Article("T", "u", "s", "su", "2024", "d", 1.0);
        SourceInfo s = new SourceInfo(
                "id", "ABC", "desc", "url", "cat", "en", "us", List.of(a)
        );

        SourceMessages.SourcesResult msg =
                new SourceMessages.SourcesResult(List.of(s));

        assertEquals(1, msg.sources().size());
        assertEquals("ABC", msg.sources().get(0).name());
    }
}
