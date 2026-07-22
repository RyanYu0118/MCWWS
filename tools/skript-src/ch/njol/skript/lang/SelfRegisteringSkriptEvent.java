/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 */
package ch.njol.skript.lang;

import ch.njol.skript.config.Config;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.Trigger;
import java.util.Objects;
import org.bukkit.event.Event;

@Deprecated(since="2.7.0", forRemoval=true)
public abstract class SelfRegisteringSkriptEvent
extends SkriptEvent {
    @Deprecated(since="2.10.0", forRemoval=true)
    public abstract void register(Trigger var1);

    @Deprecated(since="2.10.0", forRemoval=true)
    public abstract void unregister(Trigger var1);

    @Deprecated(since="2.10.0", forRemoval=true)
    public abstract void unregisterAll();

    @Override
    public boolean load() {
        boolean load = super.load();
        if (load) {
            this.afterParse(Objects.requireNonNull(this.getParser().getCurrentScript()).getConfig());
        }
        return load;
    }

    @Override
    public boolean postLoad() {
        this.register(this.trigger);
        return true;
    }

    @Override
    public void unload() {
        this.unregister(this.trigger);
    }

    @Override
    public final boolean check(Event e) {
        throw new UnsupportedOperationException();
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public void afterParse(Config config) {
    }

    @Override
    public boolean isEventPrioritySupported() {
        return false;
    }
}

