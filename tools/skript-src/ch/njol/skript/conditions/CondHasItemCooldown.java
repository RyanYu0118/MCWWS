/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Has Item Cooldown")
@Description(value={"Checks whether a cooldown is active on the specified item for a specific player.\nIf the provided item has a cooldown group component specified, the cooldown group will take priority.\nOtherwise, the cooldown of the item material will be used.\n"})
@Example(value="if player has player's tool on cooldown:\n\tsend \"You can't use this item right now. Wait %item cooldown of player's tool for player%\"\n")
@RequiredPlugins(value={"MC 1.21.2 (cooldown group)"})
@Since(value={"2.8.0", "2.12 (cooldown group)"})
public class CondHasItemCooldown
extends Condition {
    private static final boolean SUPPORTS_COOLDOWN_GROUP = Skript.methodExists(HumanEntity.class, "hasCooldown", ItemStack.class);
    private Expression<Player> players;
    private Expression<ItemType> itemTypes;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.itemTypes = exprs[1];
        this.setNegated(matchedPattern > 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        ItemType[] itemTypes = this.itemTypes.getArray(event);
        return this.players.check(event, player -> SimpleExpression.check(itemTypes, itemType -> {
            if (!itemType.hasType()) {
                return false;
            }
            if (SUPPORTS_COOLDOWN_GROUP) {
                return itemType.satisfies(arg_0 -> ((Player)player).hasCooldown(arg_0));
            }
            return itemType.satisfies(item -> player.hasCooldown(item.getType()));
        }, false, this.itemTypes.getAnd()), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.HAVE, event, debug, this.players, this.itemTypes.toString(event, debug) + " on cooldown");
    }

    static {
        Skript.registerCondition(CondHasItemCooldown.class, "%players% (has|have) [([an] item|a)] cooldown (on|for) %itemtypes%", "%players% (has|have) %itemtypes% on [(item|a)] cooldown", "%players% (doesn't|does not|do not|don't) have [([an] item|a)] cooldown (on|for) %itemtypes%", "%players% (doesn't|does not|do not|don't) have %itemtypes% on [(item|a)] cooldown");
    }
}

