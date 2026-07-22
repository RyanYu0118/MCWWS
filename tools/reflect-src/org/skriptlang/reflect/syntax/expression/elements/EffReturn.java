/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Effect
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.TriggerSection
 *  ch.njol.skript.log.ErrorQuality
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.skript.sections.SecLoop
 *  ch.njol.skript.sections.SecWhile
 *  ch.njol.util.Kleenean
 *  ch.njol.util.coll.CollectionUtils
 *  org.bukkit.event.Event
 *  org.skriptlang.skript.lang.structure.Structure
 */
package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.sections.SecLoop;
import ch.njol.skript.sections.SecWhile;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.skript.custom.Continuable;
import com.btk5h.skriptmirror.skript.reflect.sections.SectionEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.expression.ConstantGetEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionSyntaxInfo;
import org.skriptlang.reflect.syntax.expression.elements.StructCustomExpression;
import org.skriptlang.skript.lang.structure.Structure;

public class EffReturn
extends Effect {
    private Expression<?> objects;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        StructCustomExpression customExpressionSection;
        ExpressionSyntaxInfo which;
        Class<?> returnType;
        Expression expr = SkriptUtil.defendExpression(exprs[0]);
        if (!SkriptUtil.canInitSafely(expr)) {
            Skript.error((String)("Can't understand this expression: " + String.valueOf(expr)));
            return false;
        }
        boolean isContinuable = CollectionUtils.containsAnySuperclass((Class[])new Class[]{Continuable.class}, (Class[])this.getParser().getCurrentEvents());
        if (!this.getParser().isCurrentEvent(new Class[]{ExpressionGetEvent.class, ConstantGetEvent.class, SectionEvent.class}) && !isContinuable) {
            Skript.error((String)"The return effect can only be used in functions, custom expressions, sections, custom syntax parse sections and custom conditions");
            return false;
        }
        if (isContinuable && ((expr = expr.getConvertedExpression(new Class[]{Boolean.class})) == null || !expr.isSingle())) {
            Skript.error((String)(String.valueOf(exprs[0]) + " is not a single boolean value"));
            return false;
        }
        Structure structure = this.getParser().getCurrentStructure();
        if (expr != null && structure instanceof StructCustomExpression && (returnType = StructCustomExpression.returnTypes.get(which = (ExpressionSyntaxInfo)(customExpressionSection = (StructCustomExpression)structure).getFirstWhich())) != null) {
            Expression newExpr = expr.getConvertedExpression(new Class[]{returnType});
            if (newExpr == null) {
                Skript.error((String)(String.valueOf(expr) + " is not " + Classes.getSuperClassInfo(returnType).getName().withIndefiniteArticle()));
                return false;
            }
            expr = newExpr;
        }
        if (!isDelayed.isFalse()) {
            Skript.error((String)"Return may not be used if the code before it contains any delays", (ErrorQuality)ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        this.objects = expr;
        return true;
    }

    protected TriggerItem walk(Event e) {
        if (e instanceof SectionEvent) {
            ((SectionEvent)e).setOutput(this.objects == null ? new Object[]{} : this.objects.getArray(e));
        } else if (e instanceof ExpressionGetEvent) {
            ((ExpressionGetEvent)e).setOutput(this.objects == null ? new Object[]{} : this.objects.getArray(e));
        } else {
            boolean b = Boolean.TRUE.equals(this.objects.getSingle(e));
            ((Continuable)e).setContinue(b);
        }
        for (TriggerSection parent = this.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof SecLoop) {
                ((SecLoop)parent).exit(e);
                continue;
            }
            if (!(parent instanceof SecWhile)) continue;
            ((SecWhile)parent).exit(e);
        }
        return null;
    }

    protected void execute(Event e) {
        throw new UnsupportedOperationException();
    }

    public String toString(Event e, boolean debug) {
        if (this.objects == null) {
            return "return";
        }
        return "return " + this.objects.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffReturn.class, (String[])new String[]{"return [%-objects%]"});
    }
}

