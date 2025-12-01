package services;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReadabilityServiceTest {

    @Test
    public void emptyText_returnsZero() {
        assertEquals(0.0, ReadabilityService.calculateReadability(""), 0.01);
    }

    @Test
    public void simpleSentence_returnsValue() {
        double score = ReadabilityService.calculateReadability("This is a simple test sentence.");
        assertFalse(Double.isNaN(score));
    }

    @Test
    public void endingWithSilentE_doesNotCrash_andReturnsNumber() {
        double score = ReadabilityService.calculateReadability("toe.");
        // The formula may return negative or zero, so DO NOT assert >0
        assertFalse(Double.isNaN(score));
    }

    @Test
    public void multipleSentences_returnsValue() {
        double score = ReadabilityService.calculateReadability("Hello world! This is another test? Yes.");
        assertFalse(Double.isNaN(score));
    }
}
