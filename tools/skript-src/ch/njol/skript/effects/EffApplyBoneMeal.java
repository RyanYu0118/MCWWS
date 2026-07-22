/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Apply Bone Meal")
@Description(value={"Applies bone meal to a crop, sapling, or composter"})
@Example(value="apply 3 bone meal to event-block")
@RequiredPlugins(value={"MC 1.16.2+"})
@Since(value={"2.8.0"})
public class EffApplyBoneMeal
extends Effect {
    @Nullable
    private Expression<Number> amount;
    private Expression<Block> blocks;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.amount = exprs[0];
        this.blocks = exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        int times = 1;
        if (this.amount != null) {
            times = this.amount.getOptionalSingle(event).orElse(0).intValue();
        }
        for (Block block : this.blocks.getArray(event)) {
            for (int i = 0; i < times; ++i) {
                block.applyBoneMeal(BlockFace.UP);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "apply " + (this.amount != null ? this.amount.toString(event, debug) + " " : "bone meal to " + this.blocks.toString(event, debug));
    }

    static {
        Skript.registerEffect(EffApplyBoneMeal.class, "apply [%-number%] bone[ ]meal[s] [to %blocks%]");
    }
}

