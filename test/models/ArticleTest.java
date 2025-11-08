/**
 * @author Manush Shah
 */
package models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArticleTest {

    @Test
    public void setReadabilityScore_isApplied() {
        Article a = new Article(
                "t", "u", "s", "su",
                "2024-01-01 10:00", "desc", 0.0
        );
        a.setReadabilityScore(7.25);
        assertEquals(7.25, a.getReadabilityScore(), 1e-6);

        // call again to exercise the second write in the setter
        a.setReadabilityScore(0.0);
        assertEquals(0.0, a.getReadabilityScore(), 1e-6);
    }
}
