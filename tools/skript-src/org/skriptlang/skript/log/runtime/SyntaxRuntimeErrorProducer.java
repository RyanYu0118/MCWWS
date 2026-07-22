/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.log.runtime;

import ch.njol.skript.config.Node;
import ch.njol.skript.lang.SyntaxElement;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer;

public interface SyntaxRuntimeErrorProducer
extends RuntimeErrorProducer {
    public Node getNode();

    @Override
    @NotNull
    default public ErrorSource getErrorSource() {
        return ErrorSource.fromNodeAndElement(this.getNode(), (SyntaxElement)((Object)this));
    }
}

