/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package ch.njol.skript.events.bukkit;

import ch.njol.skript.config.Config;
import com.google.common.base.Preconditions;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Deprecated(since="2.10.0", forRemoval=true)
public class PreScriptLoadEvent
extends Event {
    private final List<Config> scripts;
    private static HandlerList handlers = new HandlerList();

    public PreScriptLoadEvent(List<Config> scripts) {
        super(!Bukkit.isPrimaryThread());
        Preconditions.checkNotNull(scripts);
        this.scripts = scripts;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public List<Config> getScripts() {
        return this.scripts;
    }
}

