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
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Rounding")
@Description(value={"Rounds numbers normally, up (ceiling) or down (floor) respectively."})
@Example.Examples(value={@Example(value="set {var} to rounded health of player"), @Example(value="set line 1 of the block to \"%rounded (1.5 * player's level)%\""), @Example(value="add rounded down argument to the player's health")})
@Since(value={"2.0"})
public class ExprRound
extends PropertyExpression<Number, Long> {
    private static final Patterns<RoundType> patterns = new Patterns(new Object[][]{{"[a|the] (round[ed] down|floored) %numbers%", RoundType.FLOOR}, {"%numbers% (round[ed] down|floored)", RoundType.FLOOR}, {"[a|the] round[ed] %numbers%", RoundType.ROUND}, {"%numbers% round[ed]", RoundType.ROUND}, {"[a|the] (round[ed] up|ceil[ing]ed) %numbers%", RoundType.CEIL}, {"%numbers% (round[ed] up|ceil[ing]ed)", RoundType.CEIL}});
    private RoundType roundType;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.roundType = patterns.getInfo(matchedPattern);
        return true;
    }

    protected Long @Nullable [] get(Event event, Number[] source) {
        return this.get(source, number -> {
            if (number instanceof Integer) {
                Integer integer = (Integer)number;
                return integer.longValue();
            }
            if (number instanceof Long) {
                Long long1 = (Long)number;
                return long1;
            }
            return switch (this.roundType.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> Math2.floor(number.doubleValue());
                case 1 -> Math2.round(number.doubleValue());
                case 2 -> Math2.ceil(number.doubleValue());
            };
        });
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public Expression<? extends Long> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)(switch (this.roundType.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> "floored";
            case 1 -> "rounded";
            case 2 -> "ceiled";
        }));
        builder.append((Object)this.getExpr());
        return builder.toString();
    }

    static {
        Skript.registerExpression(ExprRound.class, Long.class, ExpressionType.PROPERTY, patterns.getPatterns());
    }

    private static enum RoundType {
        FLOOR,
        ROUND,
        CEIL;

    }
}

