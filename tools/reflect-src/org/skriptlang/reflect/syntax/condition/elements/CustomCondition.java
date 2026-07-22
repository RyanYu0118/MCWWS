/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Condition
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.util.SimpleLiteral
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package org.skriptlang.reflect.syntax.condition.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.condition.ConditionSyntaxInfo;
import org.skriptlang.reflect.syntax.condition.elements.StructCustomCondition;

public class CustomCondition
extends Condition {
    private ConditionSyntaxInfo which;
    private Expression<?>[] exprs;
    private SkriptParser.ParseResult parseResult;
    private Object variablesMap;

    public boolean check(Event e) {
        Trigger checker = StructCustomCondition.conditionHandlers.get(this.which);
        if (checker == null) {
            Skript.error((String)String.format("The custom condition '%s' no longer has a check handler.", this.which.getPattern()));
            return false;
        }
        if (this.which.isProperty()) {
            return this.checkByProperty(e, checker);
        }
        return this.checkByStandard(e, checker);
    }

    private boolean checkByStandard(Event e, Trigger checker) {
        ConditionCheckEvent conditionEvent = new ConditionCheckEvent(e, this.exprs, this.which.getMatchedPattern(), this.parseResult);
        SkriptReflection.putLocals(this.variablesMap, conditionEvent);
        checker.execute((Event)conditionEvent);
        return conditionEvent.isMarkedContinue() ^ conditionEvent.isMarkedNegated() ^ this.which.isInverted();
    }

    private boolean checkByProperty(Event e, Trigger checker) {
        return this.exprs[0].check(e, o -> {
            Expression<?>[] localExprs = Arrays.copyOf(this.exprs, this.exprs.length);
            localExprs[0] = new SimpleLiteral(o, false);
            ConditionCheckEvent conditionEvent = new ConditionCheckEvent(e, localExprs, this.which.getMatchedPattern(), this.parseResult);
            SkriptReflection.putLocals(SkriptReflection.copyLocals(this.variablesMap), conditionEvent);
            checker.execute((Event)conditionEvent);
            return conditionEvent.isMarkedContinue() ^ conditionEvent.isMarkedNegated();
        }, this.which.isInverted());
    }

    public String toString(Event e, boolean debug) {
        return this.which.getPattern();
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            return false;
        }
        this.which = StructCustomCondition.lookup(SkriptUtil.getCurrentScript(), matchedPattern);
        if (this.which == null) {
            return false;
        }
        this.exprs = (Expression[])Arrays.stream(exprs).map(SkriptUtil::defendExpression).toArray(Expression[]::new);
        this.parseResult = parseResult;
        if (!SkriptUtil.canInitSafely(this.exprs)) {
            return false;
        }
        List<Supplier<Boolean>> suppliers = StructCustomCondition.usableSuppliers.get(this.which);
        if (suppliers != null && suppliers.size() != 0 && suppliers.stream().noneMatch(Supplier::get)) {
            return false;
        }
        Boolean bool = StructCustomCondition.parseSectionLoaded.get(this.which);
        if (bool != null && !bool.booleanValue()) {
            Skript.error((String)"You can't use custom conditions with parse sections before they're loaded.");
            return false;
        }
        Trigger parseHandler = StructCustomCondition.parserHandlers.get(this.which);
        if (parseHandler != null) {
            SyntaxParseEvent event = new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, this.getParser().getCurrentEvents());
            TriggerItem.walk((TriggerItem)parseHandler, (Event)event);
            this.variablesMap = SkriptReflection.removeLocals(event);
            return event.isMarkedContinue();
        }
        return true;
    }
}

