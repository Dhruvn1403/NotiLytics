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


// @Author: Varun Oza
@Singleton
public class WordStatsService {

    private final NewsApiClient newsApiClient;

    private static final Pattern WORD_TOKEN = Pattern.compile("[\\p{L}\\p{N}']+");

    @Inject
    public WordStatsService(NewsApiClient newsApiClient) {
        this.newsApiClient = newsApiClient;
    }

  
    public CompletionStage<List<WordCount>> computeWordStats(String query) {
        final int LIMIT = 50;

       
        return newsApiClient.searchArticles(query, LIMIT) 
                .thenApply(articles -> {
                    if (articles == null || articles.isEmpty()) {
                        return Collections.emptyList();
                    }

                    Map<String, Long> freq =
                            articles.stream()
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

    private Stream<String> tokenizeToWords(String text) {
        String cleaned = text
                .replaceAll("https?://\\S+", " ")
                .replaceAll("<[^>]+>", " ");
        return WORD_TOKEN.matcher(cleaned).results().map(m -> m.group());
    }

    private String normalizeToken(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        String folded = Normalizer.normalize(lower, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");
        return folded.replaceAll("^'+|'+$", "");
    }

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
