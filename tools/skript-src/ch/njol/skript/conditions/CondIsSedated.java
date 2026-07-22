/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Beehive
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

@Name(value="Beehive Is Sedated")
@Description(value={"Checks if a beehive is sedated from a nearby campfire."})
@Example(value="if {_beehive} is sedated:")
@Since(value={"2.11"})
public class CondIsSedated
extends PropertyCondition<Block> {
    @Override
    public boolean check(Block block) {
        BlockState blockState = block.getState();
        if (!(blockState instanceof Beehive)) {
            return false;
        }
        Beehive beehive = (Beehive)blockState;
        return beehive.isSedated();
    }

    @Override
    protected String getPropertyName() {
        return "sedated";
    }

    static {
        PropertyCondition.register(CondIsSedated.class, PropertyCondition.PropertyType.BE, "sedated", "blocks");
    }
}

