/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptEvent
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.parser.ParserInstance
 *  ch.njol.skript.lang.util.SimpleEvent
 *  ch.njol.skript.lang.util.SimpleLiteral
 *  org.bukkit.event.HandlerList
 */
package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.lang.util.SimpleLiteral;
import com.btk5h.skriptmirror.skript.custom.Continuable;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.event.HandlerList;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;

public class SyntaxParseEvent
extends CustomSyntaxEvent
implements Continuable {
    private static final HandlerList handlers = new HandlerList();
    private final Class<?>[] eventClasses;
    private boolean markedContinue = false;

    public SyntaxParseEvent(Expression<?>[] expressions, int matchedPattern, SkriptParser.ParseResult parseResult, Class<?>[] eventClasses) {
        super(null, SyntaxParseEvent.wrapRawExpressions(expressions), matchedPattern, parseResult);
        this.eventClasses = eventClasses;
    }

    private static Expression<?>[] wrapRawExpressions(Expression<?>[] expressions) {
        return (Expression[])Arrays.stream(expressions).map(expr -> expr == null ? null : new SimpleLiteral(expr, false)).toArray(Expression[]::new);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Class<?>[] getEventClasses() {
        return this.eventClasses;
    }

    public boolean isMarkedContinue() {
        return this.markedContinue;
    }

    @Override
    public void setContinue(boolean b) {
        this.markedContinue = b;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static <T extends CustomSyntaxStructure.SyntaxData> void register(SectionNode parseNode, List<T> whichInfo, Map<T, Trigger> parserHandlers) {
        ParserInstance.get().setCurrentEvent("custom syntax parser", new Class[]{SyntaxParseEvent.class});
        List<TriggerItem> items = SkriptUtil.getItemsFromNode(parseNode);
        whichInfo.forEach(which -> parserHandlers.put(which, new Trigger(ParserInstance.get().getCurrentScript(), "parse " + which.getPattern(), (SkriptEvent)new SimpleEvent(), items)));
    }
}

