/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@NoDoc
public class ExprEventExpression
extends WrapperExpression<Object> {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        EventValueExpression<Object> eventValue;
        String input = parseResult.regexes.getFirst().group(0);
        ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(input);
        if (classInfo == null) {
            eventValue = new EventValueExpression(input);
        } else {
            Class<?> type = classInfo.getC();
            boolean plural = Utils.getEnglishPlural(input).getSecond();
            eventValue = new EventValueExpression((Class<?>)(plural ? type.arrayType() : type));
        }
        this.setExpr(eventValue);
        return eventValue.init();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprEventExpression.class, Object.class, ExpressionType.PROPERTY, "[the] event-<.+>");
    }
}

