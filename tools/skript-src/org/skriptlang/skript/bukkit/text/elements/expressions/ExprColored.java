/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 */
package org.skriptlang.skript.bukkit.text.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Colored/Formatted/Uncolored")
@Description(value={"Parses or removes colors and, optionally, chat styles in/from a message."})
@Example.Examples(value={@Example(value="on chat:\n\tset message to colored message # only safe tags, such as colors, will be parsed\n"), @Example(value="command /fade <player>:\n\ttrigger:\n\t\tset the display name of the player-argument to the uncolored display name of the player-argument\n"), @Example(value="command /format <text>:\n\ttrigger:\n\t\tmessage formatted text-argument # parses all tags, but this is okay as the output is sent back to the executor\n")})
@Since(value={"2.0", "2.15 ('uncolored' vs 'unformatted' distinction)"})
public class ExprColored
extends SimplePropertyExpression<String, Object> {
    private boolean isColor;
    private boolean isFormat;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprColored.class, Object.class).supplier(ExprColored::new)).addPatterns("[negated:(un|non)[-]](colo[u]r-|colo[u]red )%strings%", "[negated:(un|non)[-]](format-|formatted )%strings%")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isColor = !parseResult.hasTag("negated");
        this.isFormat = matchedPattern == 1;
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public Object convert(String string) {
        TextComponentParser parser = TextComponentParser.instance();
        if (this.isColor) {
            return this.isFormat ? parser.parse(string) : parser.parseSafe(string);
        }
        return this.isFormat ? parser.stripFormatting(string) : parser.stripSafeFormatting(string);
    }

    @Override
    public Class<?> getReturnType() {
        return this.isColor ? Component.class : String.class;
    }

    @Override
    protected String getPropertyName() {
        if (this.isColor) {
            return this.isFormat ? "formatted" : "colored";
        }
        return this.isFormat ? "unformatted" : "uncolored";
    }

    @Deprecated(since="2.15", forRemoval=true)
    public boolean isUnsafeFormat() {
        return this.isColor && this.isFormat;
    }
}

