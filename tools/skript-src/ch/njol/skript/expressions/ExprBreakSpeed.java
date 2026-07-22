/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Block Break Speed")
@Description(value={"Gets the speed at which the given player would break this block, taking into account tools, potion effects, whether or not the player is in water, enchantments, etc. The returned value is the amount of progress made in breaking the block each tick. When the total breaking progress reaches 1.0, the block is broken. Note that the break speed can change in the course of breaking a block, e.g. if a potion effect is applied or expires, or the player jumps/enters water."})
@Example(value="on left click using diamond pickaxe:\n\tevent-block is set\n\tsend \"Break Speed: %break speed for player%\" to player\n")
@Since(value={"2.7"})
@RequiredPlugins(value={"1.17+"})
public class ExprBreakSpeed
extends SimpleExpression<Float> {
    private Expression<Block> blocks;
    private Expression<Player> players;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.blocks = exprs[0];
        this.players = exprs[1];
        return true;
    }

    @Nullable
    protected Float[] get(Event event) {
        ArrayList<Float> speeds = new ArrayList<Float>();
        for (Block block : this.blocks.getArray(event)) {
            for (Player player : this.players.getArray(event)) {
                speeds.add(Float.valueOf(block.getBreakSpeed(player)));
            }
        }
        return speeds.toArray(new Float[0]);
    }

    @Override
    public boolean isSingle() {
        return this.blocks.isSingle() && this.players.isSingle();
    }

    @Override
    public Class<? extends Float> getReturnType() {
        return Float.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "break speed of " + this.blocks.toString(event, debug) + " for " + this.players.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprBreakSpeed.class, Float.class, ExpressionType.COMBINED, "[the] break speed[s] [of %blocks%] [for %players%]", "%block%'[s] break speed[s] [for %players%]");
    }
}

