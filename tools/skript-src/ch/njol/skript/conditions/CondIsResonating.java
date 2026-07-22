/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Bell
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.block.Bell;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

@Name(value="Bell Is Resonating")
@Description(value={"Checks to see if a bell is currently resonating.", "A bell will start resonating five game ticks after being rung, and will continue to resonate for 40 game ticks."})
@Example(value="target block is resonating")
@Since(value={"2.9.0"})
public class CondIsResonating
extends PropertyCondition<Block> {
    @Override
    public boolean check(Block value) {
        BlockState state = value.getState(false);
        return state instanceof Bell && ((Bell)state).isResonating();
    }

    @Override
    protected String getPropertyName() {
        return "resonating";
    }

    static {
        if (Skript.classExists("org.bukkit.block.Bell") && Skript.methodExists(Bell.class, "isResonating", new Class[0])) {
            CondIsResonating.register(CondIsResonating.class, "resonating", "blocks");
        }
    }
}

