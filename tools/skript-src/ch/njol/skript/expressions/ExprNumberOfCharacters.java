/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.Iterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Number of Characters")
@Description(value={"The number of uppercase, lowercase, or digit characters in a string."})
@Example(value="#Simple Chat Filter\non chat:\n\tif number of uppercase chars in message / length of message > 0.5\n\t\tcancel event\n\t\tsend \"&lt;red&gt;Your message has to many caps!\" to player\n")
@Since(value={"2.5"})
public class ExprNumberOfCharacters
extends SimpleExpression<Long> {
    private int pattern = 0;
    private Expression<String> expr;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.pattern = matchedPattern;
        this.expr = exprs[0];
        return true;
    }

    @Nullable
    protected Long[] get(Event e) {
        String str = this.expr.getSingle(e);
        if (str == null) {
            return null;
        }
        long size = 0L;
        if (this.pattern == 0) {
            Iterator iterator = ((Iterable)str.codePoints()::iterator).iterator();
            while (iterator.hasNext()) {
                int c = (Integer)iterator.next();
                if (!Character.isUpperCase(c)) continue;
                ++size;
            }
        } else if (this.pattern == 1) {
            Iterator iterator = ((Iterable)str.codePoints()::iterator).iterator();
            while (iterator.hasNext()) {
                int c = (Integer)iterator.next();
                if (!Character.isLowerCase(c)) continue;
                ++size;
            }
        } else {
            Iterator iterator = ((Iterable)str.codePoints()::iterator).iterator();
            while (iterator.hasNext()) {
                int c = (Integer)iterator.next();
                if (!Character.isDigit(c)) continue;
                ++size;
            }
        }
        return new Long[]{size};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public Expression<? extends Long> simplify() {
        if (this.expr instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (this.pattern == 0) {
            return "number of uppercase characters";
        }
        if (this.pattern == 1) {
            return "number of lowercase characters";
        }
        return "number of digits";
    }

    static {
        Skript.registerExpression(ExprNumberOfCharacters.class, Long.class, ExpressionType.SIMPLE, "number of upper[ ]case char(acters|s) in %string%", "number of lower[ ]case char(acters|s) in %string%", "number of digit char(acters|s) in %string%");
    }
}

