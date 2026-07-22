/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ReturnHandler;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@NoDoc
public class SecReturnable
extends Section
implements ReturnHandler<Object> {
    private ClassInfo<?> returnValueType;
    private boolean singleReturnValue;
    private static Object @Nullable [] returnedValues;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        this.returnValueType = (ClassInfo)((Literal)expressions[0]).getSingle();
        this.singleReturnValue = !parseResult.hasTag("plural");
        this.loadReturnableSectionCode(sectionNode);
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        return this.walk(event, true);
    }

    @Override
    public void returnValues(Event event, Expression<?> value) {
        returnedValues = value.getArray(event);
    }

    @Override
    public boolean isSingleReturnValue() {
        return this.singleReturnValue;
    }

    @Override
    @Nullable
    public Class<?> returnValueType() {
        return this.returnValueType.getC();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "returnable " + (this.singleReturnValue ? "" : "plural ") + this.returnValueType.toString(event, debug) + " section";
    }

    static {
        Skript.registerSection(SecReturnable.class, "returnable [:plural] %*classinfo% section");
    }

    @NoDoc
    public static class ExprLastReturnValues
    extends SimpleExpression<Object> {
        @Override
        public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
            return true;
        }

        @Override
        @Nullable
        public Object[] get(Event event) {
            Object[] returnedValues = SecReturnable.returnedValues;
            SecReturnable.returnedValues = null;
            return returnedValues;
        }

        @Override
        public boolean isSingle() {
            return false;
        }

        @Override
        public Class<?> getReturnType() {
            return Object.class;
        }

        @Override
        public String toString(@Nullable Event event, boolean debug) {
            return "last returned values";
        }

        static {
            Skript.registerExpression(ExprLastReturnValues.class, Object.class, ExpressionType.SIMPLE, "[the] last return[ed] value[s]");
        }
    }
}

