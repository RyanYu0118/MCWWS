/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Variable
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;

public class ExprCustomEvent
extends SimpleExpression<Event> {
    private Expression<String> customEventName;
    private Variable<?> eventValueVarList;
    private Variable<?> dataVarList;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression<?> expr;
        if (!SkriptUtil.canInitSafely(exprs)) {
            return false;
        }
        this.customEventName = SkriptUtil.defendExpression(exprs[0]);
        if (matchedPattern == 1 && exprs[1] != null) {
            Expression var = SkriptUtil.defendExpression(exprs[1]);
            if (var instanceof Variable && ((Variable)var).isList()) {
                this.eventValueVarList = (Variable)var;
            } else {
                Skript.error((String)(var.toString(null, false) + " is not a list variable."));
                return false;
            }
        }
        if ((expr = exprs[matchedPattern == 0 ? 1 : 2]) != null) {
            Expression var = SkriptUtil.defendExpression(expr);
            if (var instanceof Variable && ((Variable)var).isList()) {
                this.dataVarList = (Variable)var;
            } else {
                Skript.error((String)(var.toString(null, false) + " is not a list variable."));
                return false;
            }
        }
        return SkriptUtil.canInitSafely(new Expression[]{this.customEventName, this.eventValueVarList, this.dataVarList});
    }

    @Nullable
    protected Event[] get(Event e) {
        String name = (String)this.customEventName.getSingle(e);
        if (name == null) {
            return null;
        }
        BukkitCustomEvent bukkitCustomEvent = new BukkitCustomEvent(name);
        if (this.eventValueVarList != null) {
            this.eventValueVarList.variablesIterator(e).forEachRemaining(pair -> {
                if (pair.getKey() == null) {
                    return;
                }
                ClassInfo classInfo = Classes.getClassInfoFromUserInput((String)((String)pair.getKey()));
                bukkitCustomEvent.setEventValue(classInfo, pair.getValue());
            });
        }
        if (this.dataVarList != null) {
            this.dataVarList.variablesIterator(e).forEachRemaining(pair -> bukkitCustomEvent.setData((String)pair.getKey(), pair.getValue()));
        }
        return new Event[]{bukkitCustomEvent};
    }

    public boolean isSingle() {
        return true;
    }

    public Class<? extends Event> getReturnType() {
        return Event.class;
    }

    public String toString(@Nullable Event e, boolean debug) {
        return "new custom event " + this.customEventName.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprCustomEvent.class, Event.class, (ExpressionType)ExpressionType.PATTERN_MATCHES_EVERYTHING, (String[])new String[]{"[a] [new] custom event %string% [(with|using)] data %-objects%", "[a] [new] custom event %string% [(with|using) [[event-]values] %-objects%] [[and] [(with|using)] data %-objects%]"});
    }
}

