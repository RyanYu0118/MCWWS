/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.util;

public class EmptyStacktraceException
extends RuntimeException {
    private static final long serialVersionUID = 5107844579323721139L;

    public EmptyStacktraceException() {
        super(null, null, true, false);
    }
}

