/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.update;

public enum ReleaseStatus {
    LATEST("latest"),
    OUTDATED("outdated"),
    UNKNOWN("unknown"),
    CUSTOM("custom"),
    DEVELOPMENT("development");

    private final String name;

    private ReleaseStatus(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}

