/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Effect
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package org.skriptlang.reflect.syntax.effect.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.effect.EffectSyntaxInfo;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.effect.elements.StructCustomEffect;

public class CustomEffect
extends Effect {
    private EffectSyntaxInfo which;
    private Expression<?>[] exprs;
    private SkriptParser.ParseResult parseResult;
    private Object variablesMap;

    protected void execute(Event e) {
        this.invokeEffect(e);
    }

    protected TriggerItem walk(Event e) {
        EffectTriggerEvent effectEvent = this.invokeEffect(e);
        if (effectEvent.isSync()) {
            return this.getNext();
        }
        Object localVars = SkriptReflection.getLocals(effectEvent.getDirectEvent());
        new Thread(() -> {
            try {
                Thread.sleep(1L);
                if (!effectEvent.hasContinued()) {
                    SkriptReflection.putLocals(localVars, effectEvent.getDirectEvent());
                }
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }).start();
        return null;
    }

    private EffectTriggerEvent invokeEffect(Event e) {
        Trigger trigger = StructCustomEffect.effectHandlers.get(this.which);
        EffectTriggerEvent effectEvent = new EffectTriggerEvent(e, this.exprs, this.which.getMatchedPattern(), this.parseResult, this.which.getPattern(), this.getNext());
        if (trigger == null) {
            Skript.error((String)String.format("The custom effect '%s' no longer has a handler.", this.which));
        } else {
            SkriptReflection.putLocals(SkriptReflection.copyLocals(this.variablesMap), effectEvent);
            trigger.execute((Event)effectEvent);
        }
        return effectEvent;
    }

    public String toString(Event e, boolean debug) {
        return this.which.getPattern();
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            return false;
        }
        this.which = StructCustomEffect.lookup(SkriptUtil.getCurrentScript(), matchedPattern);
        if (this.which == null) {
            return false;
        }
        this.exprs = (Expression[])Arrays.stream(exprs).map(SkriptUtil::defendExpression).toArray(Expression[]::new);
        this.parseResult = parseResult;
        if (!SkriptUtil.canInitSafely(this.exprs)) {
            return false;
        }
        List<Supplier<Boolean>> suppliers = StructCustomEffect.usableSuppliers.get(this.which);
        if (suppliers != null && suppliers.size() != 0 && suppliers.stream().noneMatch(Supplier::get)) {
            return false;
        }
        Boolean bool = StructCustomEffect.parseSectionLoaded.get(this.which);
        if (bool != null && !bool.booleanValue()) {
            Skript.error((String)"You can't use custom effects with parse sections before they're loaded.");
            return false;
        }
        Trigger parseHandler = StructCustomEffect.parserHandlers.get(this.which);
        if (parseHandler != null) {
            SyntaxParseEvent event = new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, this.getParser().getCurrentEvents());
            TriggerItem.walk((TriggerItem)parseHandler, (Event)event);
            this.variablesMap = SkriptReflection.removeLocals(event);
            return event.isMarkedContinue();
        }
        return true;
    }
}

