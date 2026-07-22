/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.inventory.Inventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.Version;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

@Name(value="Open/Close Inventory")
@Description(value={"Opens an inventory to a player. The player can then access and modify the inventory as if it was a chest that they just opened.", "Please note that currently 'show' and 'open' have the same effect, but 'show' will eventually show an unmodifiable view of the inventory in the future."})
@Example.Examples(value={@Example(value="show the victim's inventory to the player"), @Example(value="open the player's inventory for the player")})
@Since(value={"2.0, 2.1.1 (closing), 2.2-Fixes-V10 (anvil), 2.4 (hopper, dropper, dispenser)"})
public class EffOpenInventory
extends Effect {
    private static final Patterns<InventoryType> PATTERNS = new Patterns(new Object[][]{{"close %players%'[s] inventory [view]", null}, {"close [the] inventory [view] (to|of|for) %players%", null}, {"open %inventory/inventorytype% (to|for) %players%", null}, {"open [a] (crafting table|workbench) (to|for) %players%", InventoryType.WORKBENCH}, {"open [a] chest (to|for) %players%", InventoryType.CHEST}, {"open [a[n]] anvil (to|for) %players%", InventoryType.ANVIL}, {"open [a] hopper (to|for) %players%", InventoryType.HOPPER}, {"open [a] dropper (to|for) %players%", InventoryType.DROPPER}, {"open [a] dispenser (to|for) %players%", InventoryType.DISPENSER}});
    private static final boolean SUPPORT_MENU_TYPE;
    private boolean open;
    @Nullable
    private InventoryType inventoryType = null;
    @Nullable
    private Expression<?> inventoryExpr = null;
    private Expression<Player> players;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        InventoryType type;
        Literal lit;
        boolean bl = this.open = matchedPattern > 1;
        if (this.open) {
            if (matchedPattern == 2) {
                this.inventoryExpr = exprs[0];
            } else {
                this.inventoryType = PATTERNS.getInfo(matchedPattern);
            }
        }
        this.players = exprs[exprs.length - 1];
        Expression<Object> expression = exprs[0];
        if (expression instanceof Literal && (expression = (lit = (Literal)expression).getSingle()) instanceof InventoryType && !(type = (InventoryType)expression).isCreatable()) {
            Skript.error("Cannot create an inventory of type " + Classes.toString(type));
            return false;
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (this.inventoryExpr != null) {
            Object object = this.inventoryExpr.getSingle(event);
            this.openForPlayers(event, object);
        } else if (this.open) {
            this.openForPlayers(event, this.inventoryType);
        } else {
            for (Player player : this.players.getArray(event)) {
                player.closeInventory();
            }
        }
    }

    private void openForPlayers(Event event, Object target) {
        block4: {
            InventoryType type;
            Player[] targetPlayers;
            block3: {
                if (target == null) {
                    return;
                }
                targetPlayers = this.players.getArray(event);
                if (!(target instanceof Inventory)) break block3;
                Inventory inventory = (Inventory)target;
                for (Player player : targetPlayers) {
                    player.openInventory(inventory);
                }
                break block4;
            }
            if (!(target instanceof InventoryType) || !(type = (InventoryType)target).isCreatable()) break block4;
            for (Player player : targetPlayers) {
                this.openInventoryType(player, type);
            }
        }
    }

    private void openInventoryType(Player player, InventoryType type) {
        if (SUPPORT_MENU_TYPE && type.getMenuType() != null) {
            player.openInventory(type.getMenuType().create((HumanEntity)player, null));
            return;
        }
        player.openInventory(Bukkit.createInventory(null, (InventoryType)type));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (!this.open) {
            return "close inventory view of " + this.players.toString(event, debug);
        }
        String openedThing = this.inventoryExpr != null ? this.inventoryExpr.toString(event, debug) : (this.inventoryType != null ? this.inventoryType.name().toLowerCase().replace('_', ' ') : "inventory");
        return "open " + openedThing + " to " + this.players.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffOpenInventory.class, PATTERNS.getPatterns());
        SUPPORT_MENU_TYPE = Skript.classExists("org.bukkit.inventory.MenuType") && Skript.getMinecraftVersion().isLargerThan(new Version(1, 21, 3));
    }
}

