/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.util.Color;
import ch.njol.util.Kleenean;
import java.util.Locale;
import java.util.function.Function;

@Name(value="Alpha/Red/Green/Blue Color Value")
@Description(value={"The alpha, red, green, or blue value of colors. Ranges from 0 to 255.", "Alpha represents opacity."})
@Example.Examples(value={@Example(value="broadcast red value of rgb(100, 0, 50) # sends '100'"), @Example(value="set {_red} to red's red value + 10")})
@Keywords(value={"ARGB", "RGB", "color", "colour"})
@Since(value={"2.10"})
public class ExprARGB
extends SimplePropertyExpression<Color, Integer> {
    private RGB color;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.color = RGB.valueOf(parseResult.tags.get(0).toUpperCase(Locale.ENGLISH));
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public Integer convert(Color from) {
        return this.color.getValue(from);
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return this.color.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public Expression<? extends Integer> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    static {
        ExprARGB.register(ExprARGB.class, Integer.class, "(:alpha|:red|:green|:blue) (value|component)", "colors");
    }

    private static enum RGB {
        ALPHA(Color::getAlpha),
        RED(Color::getRed),
        GREEN(Color::getGreen),
        BLUE(Color::getBlue);

        private final Function<Color, Integer> get;

        private RGB(Function<Color, Integer> get) {
            this.get = get;
        }

        public int getValue(Color from) {
            return this.get.apply(from);
        }
    }
}

