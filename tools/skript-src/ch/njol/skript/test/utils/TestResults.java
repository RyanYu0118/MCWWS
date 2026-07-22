/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.test.utils;

import java.util.Map;
import java.util.Set;

public class TestResults {
    private final Set<String> succeeded;
    private final Map<String, String> failed;
    private final boolean docsFailed;

    public TestResults(Set<String> succeeded, Map<String, String> failed, boolean docs_failed) {
        this.docsFailed = docs_failed;
        this.succeeded = succeeded;
        this.failed = failed;
    }

    public Set<String> getSucceeded() {
        return this.succeeded;
    }

    public Map<String, String> getFailed() {
        return this.failed;
    }

    public boolean docsFailed() {
        return this.docsFailed;
    }

    public String createReport() {
        StringBuilder sb = new StringBuilder("Succeeded:\n");
        if (this.succeeded.isEmpty()) {
            sb.append("<reset> - none\n");
        }
        for (String string : this.succeeded) {
            sb.append("<reset> - <light green>").append(string).append('\n');
        }
        sb.append("<reset>Failed:\n");
        if (this.failed.isEmpty()) {
            sb.append("<reset> - none");
        }
        for (Map.Entry entry : this.failed.entrySet()) {
            sb.append("<reset> - <light red>").append((String)entry.getKey()).append("<reset>: <gray>").append((String)entry.getValue()).append('\n');
        }
        return sb.toString();
    }
}

