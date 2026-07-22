/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Item Cooldown")
@Description(value={"Gets the current cooldown of a provided item for a player.\nIf the provided item has a cooldown group component specified the cooldown of the group will be prioritized.\nOtherwise the cooldown of the item material will be used.\n"})
@Example(value="on right click using stick:\n\tset item cooldown of player's tool for player to 1 minute\n\tset item cooldown of stone and grass for all players to 20 seconds\n\treset item cooldown of cobblestone and dirt for all players\n")
@RequiredPlugins(value={"MC 1.21.2 (cooldown group)"})
@Since(value={"2.8.0", "2.12 (cooldown group)"})
public class ExprItemCooldown
extends SimpleExpression<Timespan> {
    private static final boolean SUPPORTS_COOLDOWN_GROUP = Skript.methodExists(HumanEntity.class, "getCooldown", ItemStack.class);
    private Expression<Player> players;
    private Expression<ItemType> itemtypes;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[matchedPattern ^ 1];
        this.itemtypes = exprs[matchedPattern];
        return true;
    }

    protected Timespan[] get(Event event) {
        Player[] players = this.players.getArray(event);
        List<ItemStack> itemStacks = this.convertToItemList(this.itemtypes.getArray(event));
        ArrayList<Timespan> timespans = new ArrayList<Timespan>();
        for (Player player : players) {
            for (ItemStack item : itemStacks) {
                if (SUPPORTS_COOLDOWN_GROUP) {
                    timespans.add(new Timespan(Timespan.TimePeriod.TICK, player.getCooldown(item)));
                    continue;
                }
                timespans.add(new Timespan(Timespan.TimePeriod.TICK, player.getCooldown(item.getType())));
            }
        }
        return (Timespan[])timespans.toArray(Timespan[]::new);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return mode == Changer.ChangeMode.REMOVE_ALL ? null : CollectionUtils.array(Timespan.class);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int ticks;
        if (mode != Changer.ChangeMode.RESET && mode != Changer.ChangeMode.DELETE && delta == null) {
            return;
        }
        int n = ticks = delta != null ? (int)((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK) : 0;
        if (mode == Changer.ChangeMode.REMOVE && ticks != 0) {
            ticks = -ticks;
        }
        Player[] players = this.players.getArray(event);
        List<ItemStack> itemStacks = this.convertToItemList(this.itemtypes.getArray(event));
        for (Player player : players) {
            for (ItemStack itemStack : itemStacks) {
                Material material = itemStack.getType();
                switch (mode) {
                    case RESET: 
                    case DELETE: 
                    case SET: {
                        if (SUPPORTS_COOLDOWN_GROUP) {
                            player.setCooldown(itemStack, ticks);
                            break;
                        }
                        player.setCooldown(material, ticks);
                        break;
                    }
                    case ADD: 
                    case REMOVE: {
                        if (SUPPORTS_COOLDOWN_GROUP) {
                            player.setCooldown(itemStack, Math.max(player.getCooldown(itemStack) + ticks, 0));
                            break;
                        }
                        player.setCooldown(material, Math.max(player.getCooldown(material) + ticks, 0));
                    }
                }
            }
        }
    }

    private List<ItemStack> convertToItemList(ItemType ... itemTypes) {
        return Arrays.stream(itemTypes).filter(ItemType::hasType).map(ItemType::getAll).flatMap(iterator -> {
            ArrayList itemStacks = new ArrayList();
            iterator.forEach(itemStacks::add);
            return itemStacks.stream();
        }).distinct().toList();
    }

    @Override
    public boolean isSingle() {
        return this.players.isSingle() && this.itemtypes.isSingle();
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "cooldown of " + this.itemtypes.toString(event, debug) + " for " + this.players.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprItemCooldown.class, Timespan.class, ExpressionType.COMBINED, "[the] [item] cooldown of %itemtypes% for %players%", "%players%'[s] [item] cooldown for %itemtypes%");
    }
}

