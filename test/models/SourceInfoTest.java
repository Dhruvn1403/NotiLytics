/**
 * @author Java Developers
 */
package models;

import org.junit.Test;
import java.util.List;

import static org.junit.Assert.*;

public class SourceInfoTest {

    @Test
    public void record_accessors_work() {
        Article a = new Article(
                "T1",
                "https://a.example/1",
                "Src",
                "https://src.example",
                "2025-11-07 12:00",
                "desc",
                0.0
        );

        // SourceInfo is a Java *record*, so accessors are id(), name(), ...
        SourceInfo s = new SourceInfo(
                "id-1",
                "ABC News",
                "desc",
                "https://abc.example",
                "general",
                "en",
                "us",
                List.of(a)
        );

        assertEquals("id-1", s.id());
        assertEquals("ABC News", s.name());
        assertEquals("desc", s.description());
        assertEquals("https://abc.example", s.url());
        assertEquals("general", s.category());
        assertEquals("en", s.language());
        assertEquals("us", s.country());
        assertNotNull(s.articles());
        assertEquals(1, s.articles().size());
        assertEquals("T1", s.articles().get(0).getTitle());
    }
}
