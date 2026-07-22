/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.EntityBlockStorage
 *  org.bukkit.entity.Bee
 *  org.bukkit.entity.Entity
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
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EntityBlockStorage;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Release From Entity Storage")
@Description(value={"Releases the stored entities in an entity block storage (i.e. beehive).", "When using beehives, providing a timespan will prevent the released bees from re-entering the beehive for that amount of time.", "Due to unstable behaviour on older versions, this effect requires Minecraft version 1.21+."})
@Example.Examples(value={@Example(value="release the stored entities of {_beehive}"), @Example(value="release the entity storage of {_hive} for 5 seconds")})
@RequiredPlugins(value={"Minecraft 1.21"})
@Since(value={"2.11"})
public class EffReleaseEntityStorage
extends Effect {
    private Expression<Block> blocks;
    @Nullable
    private Expression<Timespan> timespan;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.blocks = exprs[0];
        if (exprs[1] != null) {
            this.timespan = exprs[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Timespan time;
        Integer ticks = null;
        if (this.timespan != null && (time = this.timespan.getSingle(event)) != null) {
            ticks = (int)time.getAs(Timespan.TimePeriod.TICK);
        }
        for (Block block : this.blocks.getArray(event)) {
            BlockState blockState = block.getState();
            if (!(blockState instanceof EntityBlockStorage)) continue;
            EntityBlockStorage blockStorage = (EntityBlockStorage)blockState;
            List released = blockStorage.releaseEntities();
            if (ticks == null) continue;
            for (Entity entity : released) {
                if (!(entity instanceof Bee)) continue;
                Bee bee = (Bee)entity;
                bee.setCannotEnterHiveTicks(ticks.intValue());
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("release the stored entities of", this.blocks);
        if (this.timespan != null) {
            builder.append("for", this.timespan);
        }
        return builder.toString();
    }

    static {
        if (Skript.isRunningMinecraft(1, 21, 0)) {
            Skript.registerEffect(EffReleaseEntityStorage.class, "(release|evict) [the] (stored entities|entity storage) of %blocks% [for %-timespan%]");
        }
    }
}

