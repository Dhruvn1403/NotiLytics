package messages;

import models.SourceInfo;
import java.util.List;

public class SourceMessages {
    public record FetchSources(String country, String category, String language) {}
    public record SourcesResult(List<SourceInfo> sources) {}
}
