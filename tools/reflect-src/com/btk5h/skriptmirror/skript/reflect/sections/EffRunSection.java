/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.effects.Delay
 *  ch.njol.skript.lang.Effect
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionList
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.Variable
 *  ch.njol.util.Kleenean
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package com.btk5h.skriptmirror.skript.reflect.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.Variable;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.reflect.sections.Section;
import com.btk5h.skriptmirror.skript.reflect.sections.SectionEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class EffRunSection
extends Effect {
    private Expression<Section> sectionExpression;
    private Kleenean runsAsync;
    private List<Expression<?>> arguments;
    private Expression<?> variableStorage;
    private boolean shouldWait;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.sectionExpression = SkriptUtil.defendExpression(exprs[0]);
        this.runsAsync = (parseResult.mark & 1) != 0 ? Kleenean.FALSE : ((parseResult.mark & 2) != 0 ? Kleenean.TRUE : Kleenean.UNKNOWN);
        Expression expr = SkriptUtil.defendExpression(exprs[1]);
        this.arguments = new ArrayList();
        if (expr instanceof ExpressionList) {
            Collections.addAll(this.arguments, ((ExpressionList)expr).getExpressions());
        } else if (expr != null) {
            this.arguments.add(expr);
        }
        this.variableStorage = SkriptUtil.defendExpression(exprs[2]);
        if (this.variableStorage != null && !(this.variableStorage instanceof Variable)) {
            Skript.error((String)"The result can only be stored in a variable");
            return false;
        }
        boolean bl = this.shouldWait = (parseResult.mark & 4) != 0;
        if (!this.runsAsync.isUnknown() && !this.shouldWait && this.variableStorage != null) {
            Skript.warning((String)"You need to wait until the section is finished if you want to get a result.");
        }
        if (!this.runsAsync.isUnknown() && this.shouldWait) {
            this.getParser().setHasDelayBefore(Kleenean.TRUE);
        }
        return SkriptUtil.canInitSafely(this.variableStorage) && (this.arguments.size() == 0 || this.arguments.stream().allMatch(xva$0 -> SkriptUtil.canInitSafely(xva$0)));
    }

    protected TriggerItem walk(Event e) {
        if (this.runsAsync.isUnknown()) {
            return super.walk(e);
        }
        Section section = (Section)this.sectionExpression.getSingle(e);
        if (section == null) {
            return this.getNext();
        }
        Object[][] args = this.getArgs(e);
        boolean needsContinue = this.shouldWait && this.getNext() != null;
        Object localVars = needsContinue ? SkriptReflection.removeLocals(e) : null;
        boolean ranAsync = !Bukkit.isPrimaryThread();
        Runnable runSection = () -> {
            SkriptReflection.putLocals(localVars, e);
            SectionEvent sectionEvent = section.run(args);
            this.storeResult(sectionEvent, e);
            if (needsContinue) {
                Runnable continuation = () -> {
                    TriggerItem.walk((TriggerItem)this.getNext(), (Event)e);
                    SkriptReflection.removeLocals(e);
                };
                this.runTask(continuation, ranAsync);
            }
        };
        if (needsContinue) {
            Delay.addDelayedEvent((Event)e);
        }
        this.runTask(runSection, this.runsAsync.isTrue());
        return needsContinue ? null : this.getNext();
    }

    protected void execute(Event e) {
        Section section = (Section)this.sectionExpression.getSingle(e);
        if (section != null) {
            SectionEvent sectionEvent = section.run(this.getArgs(e));
            this.storeResult(sectionEvent, e);
        }
    }

    public String toString(@Nullable Event e, boolean debug) {
        return "run section " + this.sectionExpression.toString(e, debug);
    }

    private Object[][] getArgs(Event event) {
        Object[][] args = new Object[this.arguments.size()][];
        for (int i = 0; i < this.arguments.size(); ++i) {
            args[i] = this.arguments.get(i).getArray(event);
        }
        return args;
    }

    private void storeResult(SectionEvent sectionEvent, Event event) {
        Object[] output = sectionEvent.getOutput();
        if (this.variableStorage != null && output != null) {
            this.variableStorage.change(event, output, Changer.ChangeMode.SET);
        }
    }

    private void runTask(Runnable task, boolean async) {
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)SkriptMirror.getInstance(), task);
        } else {
            Bukkit.getScheduler().runTask((Plugin)SkriptMirror.getInstance(), task);
        }
    }

    static {
        Skript.registerEffect(EffRunSection.class, (String[])new String[]{"run section %section% [(1\u00a6sync|2\u00a6async)] [with [arguments] %-objects%] [and store [the] result in %-objects%] [(4\u00a6and wait)]"});
    }
}

