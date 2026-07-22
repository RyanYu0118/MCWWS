/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.variables;

import org.jetbrains.annotations.Nullable;

public class SerializedVariable {
    public final String name;
    @Nullable
    public final Value value;

    public SerializedVariable(String name, @Nullable Value value) {
        this.name = name;
        this.value = value;
    }

    public static final class Value {
        public final String type;
        public final byte[] data;

        public Value(String type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }
}

