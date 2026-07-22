/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package org.skriptlang.reflect.syntax.event;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptmirror.skript.custom.Continuable;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EventTriggerEvent
extends CustomSyntaxEvent
implements Continuable {
    private static final HandlerList handlers = new HandlerList();
    private final String which;
    private boolean markedContinue = false;

    public EventTriggerEvent(Event event, Expression<?>[] expressions, int matchedPattern, SkriptParser.ParseResult parseResult, String which) {
        super(event, expressions, matchedPattern, parseResult);
        this.which = which;
    }

    public String getWhich() {
        return this.which;
    }

    public boolean isMarkedContinue() {
        return this.markedContinue;
    }

    @Override
    public void setContinue(boolean b) {
        this.markedContinue = b;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}

