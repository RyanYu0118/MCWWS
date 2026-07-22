/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.banner.Pattern
 *  org.bukkit.block.banner.PatternType
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
import ch.njol.skript.util.Color;
import ch.njol.util.Kleenean;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Banner Pattern")
@Description(value={"Creates a new banner pattern."})
@Example.Examples(value={@Example(value="set {_pattern} to a creeper banner pattern colored red"), @Example(value="add {_pattern} to banner patterns of {_banneritem}"), @Example(value="remove {_pattern} from banner patterns of {_banneritem}"), @Example(value="set the 1st banner pattern of block at location(0,0,0) to {_pattern}"), @Example(value="clear the 1st banner pattern of block at location(0,0,0)")})
@Since(value={"2.10"})
public class ExprNewBannerPattern
extends SimpleExpression<Pattern> {
    private Expression<PatternType> selectedPattern;
    private Expression<Color> selectedColor;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.selectedPattern = exprs[0];
        this.selectedColor = exprs[1];
        return true;
    }

    protected Pattern @Nullable [] get(Event event) {
        Color color = this.selectedColor.getSingle(event);
        PatternType patternType = this.selectedPattern.getSingle(event);
        if (color == null || color.asDyeColor() == null || patternType == null) {
            return null;
        }
        return new Pattern[]{new Pattern(color.asDyeColor(), patternType)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<Pattern> getReturnType() {
        return Pattern.class;
    }

    @Override
    public Expression<? extends Pattern> simplify() {
        if (this.selectedPattern instanceof Literal && this.selectedColor instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "a " + this.selectedPattern.toString(event, debug) + " colored " + this.selectedColor.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprNewBannerPattern.class, Pattern.class, ExpressionType.COMBINED, "[a] %bannerpatterntype% colo[u]red %color%");
    }
}

