package services;

//    @author Dhruv Patel
public class ReadabilityService {

    // Calculate Flesch–Kincaid Grade Level for given text
    //    @author Dhruv Patel
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

    //    @author Dhruv Patel
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
