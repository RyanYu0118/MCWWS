/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.yggdrasil;

import org.jetbrains.annotations.Nullable;

public interface ClassResolver {
    @Nullable
    public Class<?> getClass(String var1);

    @Nullable
    public String getID(Class<?> var1);
}

