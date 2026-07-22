/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Chunk
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.Chunk;

@Name(value="Is Slime Chunk")
@Description(value={"Tests whether a chunk is a so-called slime chunk.", "Slimes can generally spawn in the swamp biome and in slime chunks.", "For more info, see <a href='https://minecraft.wiki/w/Slime#.22Slime_chunks.22'>the Minecraft wiki</a>."})
@Example(value="command /slimey:\n\ttrigger:\n\t\tif chunk at player is a slime chunk:\n\t\t\tsend \"Yeah, it is!\"\n\t\telse:\n\t\t\tsend \"Nope, it isn't\"\n")
@Since(value={"2.3"})
public class CondIsSlimeChunk
extends PropertyCondition<Chunk> {
    @Override
    public boolean check(Chunk chunk) {
        return chunk.isSlimeChunk();
    }

    @Override
    protected String getPropertyName() {
        return "slime chunk";
    }

    static {
        CondIsSlimeChunk.register(CondIsSlimeChunk.class, "([a] slime chunk|slime chunks|slimey)", "chunk");
    }
}

