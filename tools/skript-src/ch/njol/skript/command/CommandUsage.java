/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.command;

import ch.njol.skript.lang.VariableString;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class CommandUsage {
    private final VariableString usage;
    private final String defaultUsage;

    public CommandUsage(@Nullable VariableString usage, String defaultUsage) {
        if (usage == null) {
            defaultUsage = VariableString.quote(defaultUsage);
            usage = VariableString.newInstance(defaultUsage);
            assert (usage != null);
        }
        this.usage = usage;
        this.defaultUsage = defaultUsage;
    }

    public VariableString getRawUsage() {
        return this.usage;
    }

    public String getUsage() {
        return this.getUsage(null);
    }

    public String getUsage(@Nullable Event event) {
        if (event != null || this.usage.isSimple()) {
            return this.usage.toString(event);
        }
        return this.defaultUsage;
    }

    public String toString() {
        return this.getUsage();
    }
}

