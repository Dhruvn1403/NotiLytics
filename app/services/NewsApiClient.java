package services;

import models.SourceInfo;
import java.util.concurrent.CompletionStage;

public interface NewsApiClient {
    CompletionStage<SourceInfo> sourceProfileByName(String sourceName);
}
