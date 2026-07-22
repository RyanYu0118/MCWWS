/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package org.skriptlang.reflect.syntax.condition;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptmirror.skript.custom.Continuable;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ConditionCheckEvent
extends CustomSyntaxEvent
implements Continuable {
    private static final HandlerList handlers = new HandlerList();
    private boolean markedContinue;
    private boolean markedNegated = false;

    public ConditionCheckEvent(Event event, Expression<?>[] expressions, int matchedPattern, SkriptParser.ParseResult parseResult) {
        super(event, expressions, matchedPattern, parseResult);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public boolean isMarkedContinue() {
        return this.markedContinue;
    }

    public boolean isMarkedNegated() {
        return this.markedNegated;
    }

    @Override
    public void setContinue(boolean b) {
        this.markedContinue = b;
    }

    public void markNegated() {
        this.markedNegated = true;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}

