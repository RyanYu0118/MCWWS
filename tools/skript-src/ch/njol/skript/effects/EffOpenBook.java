/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Open Book")
@Description(value={"Opens a written book to a player."})
@Example(value="open book player's tool to player")
@RequiredPlugins(value={"Minecraft 1.14.2+"})
@Since(value={"2.5.1"})
public class EffOpenBook
extends Effect {
    private Expression<ItemType> book;
    private Expression<Player> players;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.book = exprs[0];
        this.players = exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        ItemStack itemStack;
        ItemType itemType = this.book.getSingle(e);
        if (itemType != null && (itemStack = itemType.getRandom()) != null && itemStack.getType() == Material.WRITTEN_BOOK) {
            for (Player player : this.players.getArray(e)) {
                player.openBook(itemStack);
            }
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "open book " + this.book.toString(e, debug) + " to " + this.players.toString(e, debug);
    }

    static {
        if (Skript.methodExists(Player.class, "openBook", ItemStack.class)) {
            Skript.registerEffect(EffOpenBook.class, "(open|show) book %itemtype% (to|for) %players%");
        }
    }
}

