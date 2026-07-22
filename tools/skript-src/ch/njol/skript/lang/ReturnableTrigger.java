/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ReturnHandler;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

public class ReturnableTrigger<T>
extends Trigger
implements ReturnHandler<T> {
    private final ReturnHandler<T> handler;

    public ReturnableTrigger(ReturnHandler<T> handler, @Nullable Script script, String name, SkriptEvent event, Function<ReturnHandler<T>, List<TriggerItem>> loadItems) {
        super(script, name, event, Collections.emptyList());
        this.handler = handler;
        this.setTriggerItems(loadItems.apply(this));
    }

    @Override
    public void returnValues(Event event, Expression<? extends T> value) {
        this.handler.returnValues(event, value);
    }

    @Override
    public boolean isSingleReturnValue() {
        return this.handler.isSingleReturnValue();
    }

    @Override
    @Nullable
    public Class<? extends T> returnValueType() {
        return this.handler.returnValueType();
    }
}

