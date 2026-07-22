/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.Leaves
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Persistent")
@Description(value={"Make entities, players, or leaves be persistent.", "Persistence of entities is whether they are retained through server restarts.", "Persistence of leaves is whether they should decay when not connected to a log block within 6 meters.", "Persistence of players is if the player's playerdata should be saved when they leave the server. Players' persistence is reset back to 'true' when they join the server.", "Passengers inherit the persistence of their vehicle, meaning a persistent zombie put on a non-persistent chicken will become non-persistent. This does not apply to players.", "By default, all entities are persistent."})
@Example.Examples(value={@Example(value="prevent all entities from persisting"), @Example(value="force {_leaves} to persist"), @Example(value="command /kickcheater <cheater: player>:\n\tpermission: op\n\ttrigger:\n\t\tprevent {_cheater} from persisting\n\t\tkick {_cheater}\n")})
@Since(value={"2.11"})
public class EffPersistent
extends Effect {
    private Expression<?> source;
    private boolean persist;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.source = exprs[0];
        this.persist = matchedPattern < 2 ? !parseResult.hasTag("not") : false;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Object object : this.source.getArray(event)) {
            Block block;
            BlockData blockData;
            if (object instanceof Entity) {
                Entity entity = (Entity)object;
                entity.setPersistent(this.persist);
                continue;
            }
            if (!(object instanceof Block) || !((blockData = (block = (Block)object).getBlockData()) instanceof Leaves)) continue;
            Leaves leaves = (Leaves)blockData;
            leaves.setPersistent(this.persist);
            block.setBlockData((BlockData)leaves);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.persist) {
            return "make " + this.source.toString(event, debug) + " persistent";
        }
        return "prevent " + this.source.toString(event, debug) + " from persisting";
    }

    static {
        Skript.registerEffect(EffPersistent.class, "make %entities/blocks% [:not] persist[ent]", "force %entities/blocks% to [:not] persist", "prevent %entities/blocks% from persisting");
    }
}

