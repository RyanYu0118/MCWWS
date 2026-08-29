package work.mcwws.newsarchive;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class NewsVersion {
    private final String id;
    private final String published;
    private final String title;
    private final String summary;
    private final String contentHash;
    private final Map<String, String> pages;

    public NewsVersion(
            String id,
            String published,
            String title,
            String summary,
            String contentHash,
            Map<String, String> pages) {
        this.id = Objects.requireNonNull(id, "id");
        this.published = published == null ? "" : published;
        this.title = title == null || title.isBlank() ? id : title;
        this.summary = summary == null ? "" : summary;
        this.contentHash = contentHash == null ? "" : contentHash;
        this.pages = Collections.unmodifiableMap(new LinkedHashMap<>(pages == null ? Map.of() : pages));
    }

    public String id() {
        return id;
    }

    public String published() {
        return published;
    }

    public String title() {
        return title;
    }

    public String summary() {
        return summary;
    }

    public String contentHash() {
        return contentHash;
    }

    public Map<String, String> pages() {
        return pages;
    }
}
