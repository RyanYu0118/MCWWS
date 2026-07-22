/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.EntityBlockStorage
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EntityBlockStorage;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity Storage Is Full")
@Description(value={"Checks to see if the an entity block storage (i.e beehive) is full."})
@Example(value="if the entity storage of {_beehive} is full:\n\trelease the entity storage of {_beehive}\n")
@Since(value={"2.11"})
public class CondEntityStorageIsFull
extends Condition {
    private Expression<Block> blocks;

    @Override
    public boolean init(Expression<?>[] exrps, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(matchedPattern >= 2);
        this.blocks = exrps[0];
        return true;
    }

    @Override
    public boolean check(Event event) {
        return this.blocks.check(event, block -> {
            BlockState patt0$temp = block.getState();
            if (!(patt0$temp instanceof EntityBlockStorage)) {
                return false;
            }
            EntityBlockStorage blockStorage = (EntityBlockStorage)patt0$temp;
            return blockStorage.isFull();
        }, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("the entity storage of", this.blocks);
        if (this.blocks.isSingle()) {
            builder.append((Object)"is");
        } else {
            builder.append((Object)"are");
        }
        if (this.isNegated()) {
            builder.append((Object)"not");
        }
        builder.append((Object)"full");
        return builder.toString();
    }

    static {
        Skript.registerCondition(CondEntityStorageIsFull.class, Condition.ConditionType.PROPERTY, "[the] entity storage of %blocks% (is|are) full", "%blocks%'[s] entity storage (is|are) full", "[the] entity storage of %blocks% (isn't|is not|aren't|are not) full", "%blocks%'[s] entity storage (isn't|is not|aren't|are not) full");
    }
}

