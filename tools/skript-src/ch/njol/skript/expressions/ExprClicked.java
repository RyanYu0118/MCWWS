/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.event.enchantment.EnchantItemEvent
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerInteractAtEntityEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.Inventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.slot.InventorySlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.lang.reflect.Array;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

@Name(value="Clicked Block/Entity/Inventory/Slot")
@Description(value={"The clicked block, entity, inventory, inventory slot, inventory click type or inventory action."})
@Example(value="message \"You clicked on a %type of clicked entity%!\"\nif the clicked block is a chest:\n\tshow the inventory of the clicked block to the player\n")
@Since(value={"1.0, 2.2-dev35 (more clickable things)"})
@Events(value={"click", "inventory click"})
public class ExprClicked
extends SimpleExpression<Object> {
    @Nullable
    private EntityData<?> entityType;
    @Nullable
    private ItemType itemType;
    private ClickableType clickable = ClickableType.BLOCK_AND_ITEMS;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.clickable = ClickableType.getClickable(parseResult.mark);
        switch (this.clickable.ordinal()) {
            case 1: {
                Object type;
                Object v0 = type = exprs[0] == null ? null : ((Literal)exprs[0]).getSingle();
                if (type instanceof EntityData) {
                    this.entityType = type;
                    if (this.getParser().isCurrentEvent((Class<? extends Event>)PlayerInteractEntityEvent.class) || this.getParser().isCurrentEvent((Class<? extends Event>)PlayerInteractAtEntityEvent.class)) break;
                    Skript.error("The expression 'clicked entity' may only be used in a click event");
                    return false;
                }
                this.itemType = type;
                if (this.getParser().isCurrentEvent((Class<? extends Event>)PlayerInteractEvent.class)) break;
                Skript.error("The expression 'clicked block' may only be used in a click event");
                return false;
            }
            case 2: 
            case 3: 
            case 4: 
            case 5: {
                if (this.getParser().isCurrentEvent((Class<? extends Event>)InventoryClickEvent.class)) break;
                Skript.error("The expression '" + this.clickable.getName() + "' may only be used in an inventory click event");
                return false;
            }
            case 0: {
                if (this.getParser().isCurrentEvent((Class<? extends Event>)EnchantItemEvent.class)) break;
                Skript.error("The expression 'clicked enchantment button' is only usable in an enchant event.");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Object> getReturnType() {
        return this.clickable != ClickableType.BLOCK_AND_ITEMS ? this.clickable.getClickableClass() : (this.entityType != null ? this.entityType.getType() : Block.class);
    }

    @Override
    @Nullable
    protected Object[] get(Event e) {
        if (!(e instanceof InventoryClickEvent) && this.clickable != ClickableType.BLOCK_AND_ITEMS && this.clickable != ClickableType.ENCHANT_BUTTON) {
            return null;
        }
        switch (this.clickable.ordinal()) {
            case 1: {
                if (e instanceof PlayerInteractEvent) {
                    if (this.entityType != null) {
                        return null;
                    }
                    Block block = ((PlayerInteractEvent)e).getClickedBlock();
                    if (this.itemType == null) {
                        return new Block[]{block};
                    }
                    assert (this.itemType != null);
                    if (this.itemType.isOfType(block)) {
                        return new Block[]{block};
                    }
                    return null;
                }
                if (!(e instanceof PlayerInteractEntityEvent)) break;
                if (this.entityType == null) {
                    return null;
                }
                Entity entity = ((PlayerInteractEntityEvent)e).getRightClicked();
                assert (this.entityType != null);
                if (this.entityType.isInstance(entity)) {
                    assert (this.entityType != null);
                    Object[] one = (Entity[])Array.newInstance(this.entityType.getType(), 1);
                    one[0] = entity;
                    return one;
                }
                return null;
            }
            case 4: {
                return new ClickType[]{((InventoryClickEvent)e).getClick()};
            }
            case 5: {
                return new InventoryAction[]{((InventoryClickEvent)e).getAction()};
            }
            case 3: {
                return new Inventory[]{((InventoryClickEvent)e).getClickedInventory()};
            }
            case 2: {
                Inventory invi = ((InventoryClickEvent)e).getClickedInventory();
                InventoryClickEvent event = (InventoryClickEvent)e;
                if (invi == null) break;
                return CollectionUtils.array(new InventorySlot(invi, event.getSlot(), event.getRawSlot()));
            }
            case 0: {
                if (!(e instanceof EnchantItemEvent)) break;
                return new Number[]{((EnchantItemEvent)e).whichButton() + 1};
            }
        }
        return null;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the " + (String)(this.clickable != ClickableType.BLOCK_AND_ITEMS ? this.clickable.getName() : "clicked " + String.valueOf(this.entityType != null ? this.entityType : (this.itemType != null ? this.itemType : "block")));
    }

    static {
        Skript.registerExpression(ExprClicked.class, Object.class, ExpressionType.SIMPLE, "[the] (" + ClickableType.ENCHANT_BUTTON.getSyntax(false) + ClickableType.BLOCK_AND_ITEMS.getSyntax(false) + ClickableType.SLOT.getSyntax(false) + ClickableType.INVENTORY.getSyntax(false) + ClickableType.TYPE.getSyntax(false) + ClickableType.ACTION.getSyntax(true) + ")");
    }

    private static enum ClickableType {
        ENCHANT_BUTTON(1, Number.class, "clicked enchantment button", "clicked [enchant[ment]] (button|option)"),
        BLOCK_AND_ITEMS(2, Block.class, "clicked block/itemtype/entity", "clicked (block|%-*itemtype/entitydata%)"),
        SLOT(3, Slot.class, "clicked slot", "clicked slot"),
        INVENTORY(4, Inventory.class, "clicked inventory", "clicked inventory"),
        TYPE(5, ClickType.class, "click type", "click (type|action)"),
        ACTION(6, InventoryAction.class, "inventory action", "inventory action");

        private String name;
        private String syntax;
        private Class<?> c;
        private int value;

        private ClickableType(int value, Class<?> c, String name, String syntax) {
            this.syntax = syntax;
            this.value = value;
            this.c = c;
            this.name = name;
        }

        public int getValue() {
            return this.value;
        }

        public Class<?> getClickableClass() {
            return this.c;
        }

        public String getName() {
            return this.name;
        }

        public String getSyntax(boolean last) {
            return this.value + "\u00a6" + this.syntax + (!last ? "|" : "");
        }

        public static ClickableType getClickable(int num) {
            for (ClickableType clickable : ClickableType.values()) {
                if (clickable.getValue() != num) continue;
                return clickable;
            }
            return BLOCK_AND_ITEMS;
        }
    }
}

