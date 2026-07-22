/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.block.Block;

@Name(value="Is Passable")
@Description(value={"Checks whether a block is passable.", "A block is passable if it has no colliding parts that would prevent players from moving through it.", "Blocks like tall grass, flowers, signs, etc. are passable, but open doors, fence gates, trap doors, etc. are not because they still have parts that can be collided with."})
@Example(value="if player's targeted block is passable")
@Since(value={"2.5.1"})
public class CondIsPassable
extends PropertyCondition<Block> {
    @Override
    public boolean check(Block block) {
        return block.isPassable();
    }

    @Override
    protected String getPropertyName() {
        return "passable";
    }

    static {
        CondIsPassable.register(CondIsPassable.class, "passable", "blocks");
    }
}

