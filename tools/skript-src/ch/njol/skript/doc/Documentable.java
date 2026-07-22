/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Unmodifiable
 */
package ch.njol.skript.doc;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public interface Documentable {
    @NotNull
    public String name();

    public @Unmodifiable @NotNull List<String> description();

    public @Unmodifiable @NotNull List<String> since();

    public @Unmodifiable @NotNull List<String> examples();

    public @Unmodifiable @NotNull List<String> keywords();

    public @Unmodifiable @NotNull List<String> requires();
}

