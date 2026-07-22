/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.classes;

import ch.njol.skript.classes.Parser;
import ch.njol.util.StringUtils;

public abstract class PatternedParser<T>
extends Parser<T> {
    public abstract String[] getPatterns();

    public String getCombinedPatterns() {
        Object[] patterns = this.getPatterns();
        return StringUtils.join(patterns, ", ");
    }
}

