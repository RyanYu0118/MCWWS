/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.lang.parser;

import ch.njol.skript.lang.ParseContext;
import java.util.HashSet;
import java.util.Set;

public final class LiteralParseCache {
    private final Set<Failure> failures = new HashSet<Failure>();

    public boolean contains(Failure failure) {
        return this.failures.contains(failure);
    }

    public void add(Failure failure) {
        this.failures.add(failure);
    }

    public void clear() {
        this.failures.clear();
    }

    public record Failure(String data, ParseContext context) {
    }
}

