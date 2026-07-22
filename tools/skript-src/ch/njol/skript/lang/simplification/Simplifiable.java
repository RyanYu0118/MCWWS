/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.lang.simplification;

import ch.njol.skript.lang.SyntaxElement;

public interface Simplifiable<S extends SyntaxElement> {
    public S simplify();
}

