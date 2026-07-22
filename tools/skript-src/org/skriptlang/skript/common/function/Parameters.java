/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.UnmodifiableView
 */
package org.skriptlang.skript.common.function;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.SequencedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.skriptlang.skript.common.function.Parameter;

public final class Parameters {
    private final SequencedMap<String, Parameter<?>> named;
    private final Parameter<?>[] indexed;

    public Parameters(SequencedMap<String, Parameter<?>> parameters) {
        this.named = parameters;
        this.indexed = new Parameter[parameters.size()];
        int i = 0;
        Iterator iterator = parameters.values().iterator();
        while (iterator.hasNext()) {
            Parameter parameter;
            this.indexed[i] = parameter = (Parameter)iterator.next();
            ++i;
        }
    }

    public Parameter<?> get(@NotNull String name) {
        return (Parameter)this.named.get(name);
    }

    public Parameter<?> getFirst() {
        return this.named.firstEntry().getValue();
    }

    public Parameter<?> get(int index) {
        return this.indexed[index];
    }

    public Parameter<?>[] all() {
        return Arrays.copyOf(this.indexed, this.indexed.length);
    }

    public int size() {
        return this.indexed.length;
    }

    public @UnmodifiableView SequencedMap<String, Parameter<?>> sequencedMap() {
        return Collections.unmodifiableSequencedMap(this.named);
    }
}

