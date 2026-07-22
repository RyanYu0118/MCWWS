/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptEvent
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Arrays;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.event.elements.StructCustomEvent;

public class CustomEvent
extends SkriptEvent {
    public static EventSyntaxInfo lastWhich;
    private EventSyntaxInfo which;
    private Expression<?>[] exprs;
    private SkriptParser.ParseResult parseResult;
    private Object variablesMap;

    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            return false;
        }
        this.which = StructCustomEvent.lookup(SkriptUtil.getCurrentScript(), matchedPattern);
        if (this.which == null) {
            return false;
        }
        this.exprs = (Expression[])Arrays.stream(args).map(SkriptUtil::defendExpression).toArray(Expression[]::new);
        this.parseResult = parseResult;
        if (!SkriptUtil.canInitSafely(this.exprs)) {
            return false;
        }
        Boolean bool = StructCustomEvent.parseSectionLoaded.get(this.which);
        if (bool != null && !bool.booleanValue()) {
            Skript.error((String)"You can't use custom events with parse sections before they're loaded.");
            return false;
        }
        Trigger parseHandler = StructCustomEvent.parserHandlers.get(this.which);
        if (parseHandler == null) {
            CustomEvent.setLastWhich(this.which);
            return true;
        }
        SyntaxParseEvent event = new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, this.getParser().getCurrentEvents());
        CustomEvent.setLastWhich(this.which);
        TriggerItem.walk((TriggerItem)parseHandler, (Event)event);
        this.variablesMap = SkriptReflection.removeLocals(event);
        CustomEvent.setLastWhich(this.which);
        return event.isMarkedContinue();
    }

    public boolean load() {
        CustomEvent.setLastWhich(this.which);
        boolean parsed = super.load();
        CustomEvent.setLastWhich(null);
        return parsed;
    }

    public boolean check(Event e) {
        BukkitCustomEvent bukkitCustomEvent = (BukkitCustomEvent)e;
        if (!bukkitCustomEvent.getName().equalsIgnoreCase(StructCustomEvent.nameValues.get(this.which))) {
            return false;
        }
        EventTriggerEvent eventTriggerEvent = new EventTriggerEvent(e, this.exprs, this.which.getMatchedPattern(), this.parseResult, this.which.getPattern());
        SkriptReflection.putLocals(SkriptReflection.copyLocals(this.variablesMap), eventTriggerEvent);
        Trigger trigger = StructCustomEvent.eventHandlers.get(this.which);
        if (trigger != null) {
            trigger.execute((Event)eventTriggerEvent);
            return eventTriggerEvent.isMarkedContinue();
        }
        return true;
    }

    public static void setLastWhich(EventSyntaxInfo which) {
        lastWhich = which;
    }

    public String toString(@Nullable Event e, boolean debug) {
        return this.which.getPattern();
    }
}

