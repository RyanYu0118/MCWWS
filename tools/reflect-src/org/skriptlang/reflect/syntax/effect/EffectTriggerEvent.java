/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.TriggerItem
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package org.skriptlang.reflect.syntax.effect;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EffectTriggerEvent
extends CustomSyntaxEvent {
    private static final HandlerList handlers = new HandlerList();
    private final String which;
    private final TriggerItem next;
    private boolean sync = true;
    private boolean hasContinued = false;

    public EffectTriggerEvent(Event event, Expression<?>[] expressions, int matchedPattern, SkriptParser.ParseResult parseResult, String which, TriggerItem next) {
        super(event, expressions, matchedPattern, parseResult);
        this.which = which;
        this.next = next;
    }

    public String getWhich() {
        return this.which;
    }

    public TriggerItem getNext() {
        return this.next;
    }

    public boolean isSync() {
        return this.sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public boolean hasContinued() {
        return this.hasContinued;
    }

    public void setContinued() {
        this.hasContinued = true;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}

