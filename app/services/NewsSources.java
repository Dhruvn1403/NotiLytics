package services;

import models.SourceInfo;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface NewsSources {
    CompletionStage<List<SourceInfo>> fetchSources(String country, String category, String language);
}
