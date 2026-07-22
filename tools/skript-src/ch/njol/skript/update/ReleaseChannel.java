/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.update;

import java.util.function.Function;

public class ReleaseChannel {
    private final Function<String, Boolean> checker;
    private final String name;

    public ReleaseChannel(Function<String, Boolean> checker, String name) {
        this.checker = checker;
        this.name = name;
    }

    public boolean check(String release) {
        return this.checker.apply(release);
    }

    public String getName() {
        return this.name;
    }
}

