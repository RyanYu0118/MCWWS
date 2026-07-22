/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Sign
 *  org.bukkit.block.sign.Side
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.SignChangeEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.block.sign.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.Event;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Sign Text")
@Description(value={"A line of text on a sign. Can be changed, but note that there is a 16 character limit per line."})
@Example(value="on right click:\n\tclicked block is tagged as \"minecraft:all_signs\"\n\tif line 2 of the clicked block is \"[Heal]\":\n\t\theal the player\n")
@Since(value={"1.3"})
public class ExprSignText
extends SimpleExpression<Component> {
    private Expression<Integer> line;
    private Expression<Block> block;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprSignText.class, Component.class).supplier(ExprSignText::new)).priority(PropertyExpression.DEFAULT_PRIORITY)).addPatterns("line %integer% [of %block%]", "[the] (1:1st|1:first|2:2nd|2:second|3:3rd|3:third|4:4th|4:fourth) line [of %block%]")).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.line = matchedPattern == 0 ? exprs[0] : new SimpleLiteral<Integer>(parseResult.mark, false);
        this.block = exprs[exprs.length - 1];
        return true;
    }

    private int getLine(Event event) {
        Integer line = this.line.getSingle(event);
        if (line == null) {
            return -1;
        }
        if (line < 1 || line > 4) {
            this.error("Signs only have lines from 1 to 4, but tried to obtain line " + line);
            return -1;
        }
        Integer n = line;
        line = line - 1;
        return line;
    }

    protected Component[] get(Event event) {
        BlockState blockState;
        Block block;
        int line = this.getLine(event);
        if (line == -1) {
            return new Component[0];
        }
        if (this.getTime() >= 0 && this.block.isDefault() && event instanceof SignChangeEvent) {
            SignChangeEvent signEvent = (SignChangeEvent)event;
            if (!Delay.isDelayed(event)) {
                return new Component[]{signEvent.line(line)};
            }
        }
        if ((block = this.block.getSingle(event)) == null || !((blockState = block.getState()) instanceof Sign)) {
            return new Component[0];
        }
        Sign signState = (Sign)blockState;
        return new Component[]{signState.getSide(Side.FRONT).line(line)};
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE -> CollectionUtils.array(Component.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        BlockState blockState;
        Block block;
        int line = this.getLine(event);
        if (line == -1) {
            return;
        }
        if (this.getTime() >= 0 && this.block.isDefault() && event instanceof SignChangeEvent) {
            SignChangeEvent signEvent = (SignChangeEvent)event;
            if (!Delay.isDelayed(event)) {
                signEvent.line(line, delta == null ? null : (Component)delta[0]);
                return;
            }
        }
        if ((block = this.block.getSingle(event)) == null || !((blockState = block.getState()) instanceof Sign)) {
            return;
        }
        Sign signState = (Sign)blockState;
        signState.getSide(Side.FRONT).line(line, (Component)(delta == null ? Component.empty() : (Component)delta[0]));
        signState.update(false, false);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Component> getReturnType() {
        return Component.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "line " + this.line.toString(event, debug) + " of " + this.block.toString(event, debug);
    }

    @Override
    public boolean setTime(int time) {
        return super.setTime(time, SignChangeEvent.class, this.block);
    }
}

