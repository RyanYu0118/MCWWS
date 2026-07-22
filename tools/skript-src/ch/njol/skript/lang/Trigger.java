/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.skript.variables.Variables;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

public class Trigger
extends TriggerSection {
    private final String name;
    private final SkriptEvent event;
    @Nullable
    private final Script script;
    private int line = -1;
    private String debugLabel;

    public Trigger(@Nullable Script script, String name, SkriptEvent event, List<TriggerItem> items) {
        super(items);
        this.script = script;
        this.name = name;
        this.event = event;
        this.debugLabel = "unknown trigger";
    }

    public boolean execute(Event event) {
        boolean success = TriggerItem.walk(this, event);
        Variables.removeLocals(event);
        return success;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        return this.walk(event, true);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.name + " (" + this.event.toString(event, debug) + ")";
    }

    public String getName() {
        return this.name;
    }

    public SkriptEvent getEvent() {
        return this.event;
    }

    @Nullable
    public Script getScript() {
        return this.script;
    }

    public void setLineNumber(int line) {
        this.line = line;
    }

    public int getLineNumber() {
        return this.line;
    }

    public void setDebugLabel(String label) {
        this.debugLabel = label;
    }

    public String getDebugLabel() {
        return this.debugLabel;
    }
}

