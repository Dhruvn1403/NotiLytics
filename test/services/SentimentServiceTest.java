package services;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import play.libs.Json;
import java.util.*;

import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jaiminkumar Mayani
 */

class SentimentServiceTest {

    @Test
    void happyMajorityReturnsSmiley() {
        SentimentServiceImpl svc = new SentimentServiceImpl(null, null);
        String emo = svc.aggregateSentiment(List.of("happy joy amazing wonderful great"));
        assertEquals(":-)", emo);
    }

    @Test
    void sadMajorityReturnsFrowny() {
        SentimentServiceImpl svc = new SentimentServiceImpl(null, null);
        String emo = svc.aggregateSentiment(List.of("sad terrible awful bad hate"));
        assertEquals(":-(", emo);
    }

    @Test
    void neutralWhenBalanced() {
        SentimentServiceImpl svc = new SentimentServiceImpl(null, null);
        String emo = svc.aggregateSentiment(List.of("happy sad happy sad"));
        assertEquals(":-|", emo);
    }
}