package services;

/**
 * @author Dhruv Patel
 *
 * Service class to compute readability scores for a given text.
 * Implements the Flesch–Kincaid Grade Level formula.
 * Provides static utility methods for calculating readability and syllable count.
 */
public class ReadabilityService {

    /**
     * Calculates the Flesch–Kincaid Grade Level for the given text.
     * Returns a readability score rounded to 2 decimal places.
     *
     * @param text Input text to analyze
     * @return Readability score (Flesch–Kincaid Grade Level)
     */
    public static double calculateReadability(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        int sentenceCount = text.split("[.!?]").length;
        int wordCount = text.split("\\s+").length;
        int syllableCount = countSyllables(text);

        if (wordCount == 0 || sentenceCount == 0) return 0.0;

        double fleschKincaid =
                0.39 * ((double) wordCount / sentenceCount) +
                        11.8 * ((double) syllableCount / wordCount) - 15.59;

        return Math.round(fleschKincaid * 100.0) / 100.0; // round to 2 decimals
    }

    /**
     * Counts approximate number of syllables in the given text.
     * Uses a simple heuristic: counts vowel groups, adjusts for silent 'e'.
     *
     * @param text Input text
     * @return Estimated syllable count
     */
    private static int countSyllables(String text) {
        text = text.toLowerCase();
        int count = 0;
        boolean prevVowel = false;
        for (char c : text.toCharArray()) {
            if ("aeiouy".indexOf(c) != -1) {
                if (!prevVowel) count++;
                prevVowel = true;
            } else {
                prevVowel = false;
            }
        }
        if (text.endsWith("e")) count--;
        if (count == 0) count = 1;
        return count;
    }
}
