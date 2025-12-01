package messages;

import models.Article;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SearchMessagesTest {

    @Test
    public void outerClass_isLoadable() {
        // Explicitly instantiate – required for Jacoco coverage of outer class
        new SearchMessages();
    }

    @Test
    public void startSearch_record_works() {
        SearchMessages.StartSearch msg = new SearchMessages.StartSearch("hello");
        assertEquals("hello", msg.query());
    }

    @Test
    public void newArticles_record_works() {
        Article a = new Article("T", "u", "s", "su", "2024", "d", 1.0);
        SearchMessages.NewArticles msg = new SearchMessages.NewArticles(List.of(a));

        assertEquals(1, msg.articles().size());
        assertEquals("T", msg.articles().get(0).getTitle());
    }

    @Test
    public void pollForUpdates_record_works() {
        SearchMessages.PollForUpdates msg =
                new SearchMessages.PollForUpdates("crypto");

        assertEquals("crypto", msg.query());
    }
}
