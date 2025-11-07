package models;

import services.NewsApiClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes word-frequency stats from article descriptions.
 * - Fetch up to 50 articles for a query (via NewsApiClient)
 * - Use Java Streams to tokenize & count
 * - Return sorted (desc) frequencies asynchronously
 *
 * NOTE: We depend on your existing models.Article (record) and services.NewsApiClient.
 * If your NewsApiClient method name differs, adjust the call in computeWordStats().
 *
 * Author: Your Name
 */
@Singleton
public class WordStatsService {

    private final NewsApiClient newsApiClient;

    // "word-ish" tokens (letters, digits, apostrophes)
    private static final Pattern WORD_TOKEN = Pattern.compile("[\\p{L}\\p{N}']+");

    @Inject
    public WordStatsService(NewsApiClient newsApiClient) {
        this.newsApiClient = newsApiClient;
    }

    /**
     * Compute word stats for a search phrase using article descriptions.
     */
    public CompletionStage<List<WordCount>> computeWordStats(String query) {
        final int LIMIT = 50;

        // 🔧 Change the method name below if your client uses a different one (e.g., searchEverything, searchArticles, etc.)
        // It must return CompletionStage<List<Article>> (your models.Article record).
        return newsApiClient.searchArticles(query, LIMIT) // TODO: if your client method is different, rename here
                .thenApply(articles -> {
                    if (articles == null || articles.isEmpty()) {
                        return Collections.emptyList();
                    }

                    Map<String, Long> freq =
                            articles.stream()
                                    .map(Article::getDescription)              // use the record accessor
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

    /** Split text into tokens after removing URLs and simple HTML tags. */
    private Stream<String> tokenizeToWords(String text) {
        String cleaned = text
                .replaceAll("https?://\\S+", " ")
                .replaceAll("<[^>]+>", " ");
        return WORD_TOKEN.matcher(cleaned).results().map(m -> m.group());
    }

    /** Normalize token: lowercase, fold diacritics, trim apostrophes at ends. */
    private String normalizeToken(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        String folded = Normalizer.normalize(lower, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");
        return folded.replaceAll("^'+|'+$", "");
    }

    /** DTO for (word, count). */
    public static final class WordCount {
        private final String word;
        private final long count;

        public WordCount(String word, long count) {
            this.word = word;
            this.count = count;
        }
        public String getWord() { return word; }
        public long getCount() { return count; }
    }
}
