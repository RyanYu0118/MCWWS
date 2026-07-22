/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionList
 *  ch.njol.skript.lang.Section
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.Variable
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package com.btk5h.skriptmirror.skript.reflect.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.Variable;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.reflect.sections.Section;
import com.btk5h.skriptmirror.skript.reflect.sections.SectionEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class SecSection
extends ch.njol.skript.lang.Section {
    public static boolean sectionsUsed = false;
    private Trigger trigger;
    private ArrayList<Variable<?>> variableArguments;
    private Expression<Object> variableStore;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        sectionsUsed = true;
        Expression varList = SkriptUtil.defendExpression(exprs[0]);
        this.variableStore = SkriptUtil.defendExpression(exprs[1]);
        if (!SkriptUtil.canInitSafely(varList, this.variableStore)) {
            return false;
        }
        this.variableArguments = new ArrayList();
        if (varList != null) {
            if (varList instanceof ExpressionList) {
                ExpressionList expressionList = (ExpressionList)varList;
                for (Expression expr : expressionList.getExpressions()) {
                    if (!(expr instanceof Variable)) {
                        Skript.error((String)"The arguments can only contain variables");
                        return false;
                    }
                    this.variableArguments.add((Variable)expr);
                }
            } else if (varList instanceof Variable) {
                this.variableArguments.add((Variable)varList);
            } else {
                Skript.error((String)"The arguments can only contain variables");
                return false;
            }
        }
        if (!(this.variableStore instanceof Variable)) {
            Skript.error((String)"The created section can only be stored in a variable");
            return false;
        }
        this.trigger = this.loadCode(sectionNode, "section", new Class[]{SectionEvent.class});
        return SkriptUtil.canInitSafely(this.variableStore);
    }

    @Nullable
    protected TriggerItem walk(Event e) {
        Section section = new Section(this.trigger, e, (List<Variable<?>>)this.variableArguments);
        this.variableStore.change(e, (Object[])new Section[]{section}, Changer.ChangeMode.SET);
        return super.walk(e, false);
    }

    public String toString(@Nullable Event e, boolean debug) {
        StringBuilder stringBuilder = new StringBuilder("create section");
        if (!this.variableArguments.isEmpty()) {
            stringBuilder.append(" with argument variables ");
            for (int i = 0; i < this.variableArguments.size(); ++i) {
                Variable<?> variable = this.variableArguments.get(i);
                stringBuilder.append(variable.toString(e, debug));
                if (i == this.variableArguments.size() - 2) {
                    stringBuilder.append(" and ");
                    continue;
                }
                if (i == this.variableArguments.size() - 1) continue;
                stringBuilder.append(", ");
            }
        }
        if (this.variableStore != null) {
            stringBuilder.append(" stored in ").append(this.variableStore.toString(e, debug));
        }
        return stringBuilder.toString();
    }

    static {
        Skript.registerSection(SecSection.class, (String[])new String[]{"create [new] section [with [arguments variables] %-objects%] (and store it|stored) in %objects%"});
    }
}

