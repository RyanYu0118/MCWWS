/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.util.Utils;
import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public class Version
implements Serializable,
Comparable<Version> {
    private static final long serialVersionUID = 8687040355286333293L;
    private final Integer[] version = new Integer[3];
    @Nullable
    private final String postfix;
    public static final Pattern versionPattern = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-(.*))?");

    public Version(int ... version) {
        if (version.length < 1 || version.length > 3) {
            throw new IllegalArgumentException("Versions must have a minimum of 2 and a maximum of 3 numbers (" + version.length + " numbers given)");
        }
        for (int i = 0; i < version.length; ++i) {
            this.version[i] = version[i];
        }
        this.postfix = null;
    }

    public Version(int major, int minor, @Nullable String postfix) {
        this.version[0] = major;
        this.version[1] = minor;
        this.postfix = postfix == null || postfix.isEmpty() ? null : postfix;
    }

    public Version(String version) {
        Matcher m = versionPattern.matcher(version.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("'" + version + "' is not a valid version string");
        }
        for (int i = 0; i < 3; ++i) {
            if (m.group(i + 1) == null) continue;
            this.version[i] = Utils.parseInt(m.group(i + 1));
        }
        this.postfix = m.group(4);
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Version)) {
            return false;
        }
        return this.compareTo((Version)obj) == 0;
    }

    public int hashCode() {
        return Arrays.hashCode((Object[])this.version) * 31 + (this.postfix == null ? 0 : this.postfix.hashCode());
    }

    @Override
    public int compareTo(@Nullable Version other) {
        if (other == null) {
            return 1;
        }
        for (int i = 0; i < this.version.length; ++i) {
            if (this.get(i) > other.get(i)) {
                return 1;
            }
            if (this.get(i) >= other.get(i)) continue;
            return -1;
        }
        return Version.comparePostfixes(this.postfix, other.postfix);
    }

    private static int comparePostfixes(@Nullable String postfixA, @Nullable String postfixB) {
        String[] prefixes;
        postfixA = postfixA == null ? null : postfixA.toLowerCase();
        String string = postfixB = postfixB == null ? null : postfixB.toLowerCase();
        if (postfixA == null && postfixB == null) {
            return 0;
        }
        if (postfixA == null) {
            if (postfixB.startsWith("nightly")) {
                return -1;
            }
            return 1;
        }
        if (postfixB == null) {
            if (postfixA.startsWith("nightly")) {
                return 1;
            }
            return -1;
        }
        if (postfixA.startsWith("nightly")) {
            return postfixB.startsWith("nightly") ? 0 : 1;
        }
        if (postfixB.startsWith("nightly")) {
            return -1;
        }
        for (String prefix : prefixes = new String[]{"pre", "beta", "alpha"}) {
            boolean aStarts = postfixA.startsWith(prefix);
            boolean bStarts = postfixB.startsWith(prefix);
            if (!aStarts && !bStarts) continue;
            if (aStarts && bStarts) {
                String aNumberStr = postfixA.substring(prefix.length()).trim().split("-")[0];
                String bNumberStr = postfixB.substring(prefix.length()).trim().split("-")[0];
                int aNumber = Math.abs(Utils.parseInt(aNumberStr));
                int bNumber = Math.abs(Utils.parseInt(bNumberStr));
                return Integer.compare(aNumber, bNumber);
            }
            return aStarts ? 1 : -1;
        }
        return 0;
    }

    @Override
    public int compareTo(int ... other) {
        assert (other.length >= 2 && other.length <= 3);
        for (int i = 0; i < this.version.length; ++i) {
            if (this.get(i) > (i >= other.length ? 0 : other[i])) {
                return 1;
            }
            if (this.get(i) >= (i >= other.length ? 0 : other[i])) continue;
            return -1;
        }
        return 0;
    }

    private int get(int i) {
        return this.version[i] == null ? 0 : this.version[i];
    }

    public boolean isSmallerThan(Version other) {
        return this.compareTo(other) < 0;
    }

    public boolean isLargerThan(Version other) {
        return this.compareTo(other) > 0;
    }

    public boolean isStable() {
        return this.postfix == null;
    }

    public int getMajor() {
        return this.version[0];
    }

    public int getMinor() {
        return this.version[1];
    }

    public int getRevision() {
        return this.version[2] == null ? 0 : this.version[2];
    }

    public String toString() {
        return this.version[0] + "." + this.version[1] + (String)(this.version[2] == null ? "" : "." + this.version[2]) + (String)(this.postfix == null ? "" : "-" + this.postfix);
    }

    public static int compare(String v1, String v2) {
        return new Version(v1).compareTo(new Version(v2));
    }
}

