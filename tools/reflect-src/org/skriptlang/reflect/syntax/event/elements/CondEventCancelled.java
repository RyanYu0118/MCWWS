/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.conditions.base.PropertyCondition
 *  org.bukkit.event.Cancellable
 */
package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.conditions.base.PropertyCondition;
import org.bukkit.event.Cancellable;

public class CondEventCancelled<T>
extends PropertyCondition<T> {
    public boolean check(T event) {
        return event instanceof Cancellable && ((Cancellable)event).isCancelled();
    }

    protected String getPropertyName() {
        return "cancelled";
    }

    static {
        CondEventCancelled.register(CondEventCancelled.class, (String)"cancelled", (String)"events");
    }
}

