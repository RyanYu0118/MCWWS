/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Send Block Change")
@Description(value={"Makes a player see a block as something else or as the original."})
@Example.Examples(value={@Example(value="make player see block at player as dirt"), @Example(value="make player see player's target block as campfire[facing=south]"), @Example(value="make all players see (blocks in radius 5 of location(0, 0, 0)) as bedrock\nmake all players see (blocks in radius 5 of location(0, 0, 0)) as original\n")})
@Since(value={"2.2-dev37c, 2.5.1 (block data support), 2.12 (as original)"})
public class EffSendBlockChange
extends Effect {
    private Expression<Player> players;
    private Expression<Location> locations;
    @Nullable
    private Expression<Object> type;
    private boolean asOriginal;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.locations = exprs[1];
        boolean bl = this.asOriginal = matchedPattern == 1;
        if (!this.asOriginal) {
            this.type = exprs[2];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        block10: {
            Player[] players;
            Object type;
            block9: {
                if (this.asOriginal) {
                    Player[] players2 = this.players.getArray(event);
                    for (Location location : this.locations.getArray(event)) {
                        for (Player player : players2) {
                            player.sendBlockChange(location, location.getBlock().getBlockData());
                        }
                    }
                    return;
                }
                assert (this.type != null);
                type = this.type.getSingle(event);
                if (type == null) {
                    return;
                }
                players = this.players.getArray(event);
                if (!(type instanceof ItemType)) break block9;
                ItemType itemType = (ItemType)type;
                for (Location location : this.locations.getArray(event)) {
                    for (Player player : players) {
                        itemType.sendBlockChange(player, location);
                    }
                }
                break block10;
            }
            if (!(type instanceof BlockData)) break block10;
            BlockData blockData = (BlockData)type;
            for (Location location : this.locations.getArray(event)) {
                for (Player player : players) {
                    player.sendBlockChange(location, blockData);
                }
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("make", this.players, "see", this.locations, "as");
        if (this.asOriginal) {
            builder.append((Object)"original");
        } else {
            assert (this.type != null);
            builder.append((Object)this.type);
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffSendBlockChange.class, "make %players% see %locations% as %itemtype/blockdata%", "make %players% see %locations% as [the|its] (original|normal|actual) [block]");
    }
}

