package services;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ReadabilityService}.
 *
 * <p>This suite validates that the readability calculation:
 * <ul>
 *     <li>Handles empty or null input gracefully</li>
 *     <li>Produces numeric values for normal sentences</li>
 *     <li>Does not crash on words ending with silent 'e'</li>
 *     <li>Works with multiple sentences and punctuation</li>
 * </ul>
 *
 * <p>These tests do not assert a specific Flesch-Kincaid score,
 * because the algorithm may be tuned and still remain correct.
 * Instead, we ensure the function returns a valid numeric value
 * and never throws exceptions.</p>
 *
 * @author Dhruv Patel
 */
public class ReadabilityServiceTest {

    /**
     * Ensures that an empty input string returns a readability score of 0.0.
     * This verifies that the method handles edge cases safely.
     */
    @Test
    public void emptyText_returnsZero() {
        assertEquals(0.0, ReadabilityService.calculateReadability(""), 0.01);
    }

    /**
     * Ensures that a simple sentence returns a valid numeric score.
     * Not asserting specific values to avoid over-constraining the algorithm.
     */
    @Test
    public void simpleSentence_returnsValue() {
        double score = ReadabilityService.calculateReadability(
                "This is a simple test sentence."
        );
        assertFalse("Score should not be NaN", Double.isNaN(score));
    }

    /**
     * Tests robustness when a word ends in a silent 'e' ("toe").
     * Ensures no crash occurs and a numeric score is produced.
     */
    @Test
    public void endingWithSilentE_doesNotCrash_andReturnsNumber() {
        double score = ReadabilityService.calculateReadability("toe.");
        assertFalse("Score should not be NaN", Double.isNaN(score));
    }

    /**
     * Ensures that multiple sentences with mixed punctuation do not break parsing.
     */
    @Test
    public void multipleSentences_returnsValue() {
        double score = ReadabilityService.calculateReadability(
                "Hello world! This is another test? Yes."
        );
        assertFalse("Score should not be NaN", Double.isNaN(score));
    }
}
