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

@Name(value="Bell Is Ringing")
@Description(value={"Checks to see if a bell is currently ringing. A bell typically rings for 50 game ticks."})
@Example(value="target block is ringing")
@Since(value={"2.9.0"})
public class CondIsRinging
extends PropertyCondition<Block> {
    @Override
    public boolean check(Block value) {
        BlockState state = value.getState(false);
        return state instanceof Bell && ((Bell)state).isShaking();
    }

    @Override
    protected String getPropertyName() {
        return "ringing";
    }

    static {
        if (Skript.classExists("org.bukkit.block.Bell") && Skript.methodExists(Bell.class, "isShaking", new Class[0])) {
            CondIsRinging.register(CondIsRinging.class, "ringing", "blocks");
        }
    }
}

