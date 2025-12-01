package services;

import javax.inject.Inject;
import javax.inject.Singleton;

import models.Article;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service to compute word statistics (frequency) from news article descriptions.
 * Cleans, tokenizes, and normalizes text to produce a sorted list of word counts.
 *
 * @author Varun Oza
 */
@Singleton
public class WordStatsService {

    private final NewsApiClient newsApiClient;

    // Regex pattern to match words (letters, numbers, apostrophes)
    private static final Pattern WORD_TOKEN = Pattern.compile("[\\p{L}\\p{N}']+");

    /**
     * Constructor injecting NewsApiClient to fetch articles for analysis.
     *
     * @param newsApiClient News API client service
     */
    @Inject
    public WordStatsService(NewsApiClient newsApiClient) {
        this.newsApiClient = newsApiClient;
    }

    /**
     * Computes word frequency statistics from the top articles matching a query.
     *
     * @param query Search query
     * @return CompletionStage containing a list of WordCount objects sorted by frequency
     */
    public CompletionStage<List<WordCount>> computeWordStats(String query) {
        final int LIMIT = 50; // fetch up to 50 articles

        return newsApiClient.searchArticles(query, LIMIT)
                .thenApply(articles -> {
                    if (articles == null || articles.isEmpty()) {
                        return Collections.emptyList();
                    }

                    Map<String, Long> freq = articles.stream()
                            .map(Article::getDescription)
                            .filter(Objects::nonNull)
                            .flatMap(this::tokenizeToWords)
                            .map(this::normalizeToken)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

                    return freq.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                                    .thenComparing(Map.Entry.comparingByKey()))
                            .map(e -> new WordCount(e.getKey(), e.getValue()))
                            .collect(Collectors.toList());
                });
    }

    /**
     * Tokenizes a string into words using regex, removing URLs and HTML tags first.
     *
     * @param text Input text
     * @return Stream of word tokens
     */
    private Stream<String> tokenizeToWords(String text) {
        String cleaned = text
                .replaceAll("https?://\\S+", " ") // remove URLs
                .replaceAll("<[^>]+>", " ");       // remove HTML tags
        return WORD_TOKEN.matcher(cleaned).results().map(m -> m.group());
    }

    /**
     * Normalizes a token by lowercasing, removing accents, and trimming apostrophes.
     *
     * @param token Input word token
     * @return Normalized word
     */
    private String normalizeToken(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        String folded = Normalizer.normalize(lower, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");
        return folded.replaceAll("^'+|'+$", ""); // remove leading/trailing apostrophes
    }

    /**
     * Data class representing a word and its frequency count.
     */
    public static final class WordCount {
        private final String word;
        private final long count;

        /**
         * Constructor
         * @param word The word
         * @param count Frequency of the word
         */
        public WordCount(String word, long count) {
            this.word = word;
            this.count = count;
        }

        /** Returns the word */
        public String getWord() { return word; }

        /** Returns the frequency count */
        public long getCount() { return count; }
    }
}
