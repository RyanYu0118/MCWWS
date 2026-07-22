/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 */
package ch.njol.skript.lang;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public interface KeyReceiverExpression<T>
extends Expression<T> {
    default public boolean acceptsNestedStructures() {
        return false;
    }

    public void change(Event var1, Object @NotNull [] var2, Changer.ChangeMode var3, @NotNull @NotNull String @NotNull [] var4);
}

