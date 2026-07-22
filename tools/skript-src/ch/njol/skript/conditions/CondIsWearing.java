/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Horse
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Llama
 *  org.bukkit.entity.Wolf
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.EntityEquipment
 *  org.bukkit.inventory.EquipmentSlot
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
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Wearing")
@Description(value={"Checks whether an entity is wearing some items (usually armor)."})
@Example.Examples(value={@Example(value="player is wearing an iron chestplate and iron leggings"), @Example(value="target is wearing wolf armor")})
@Since(value={"1.0"})
public class CondIsWearing
extends Condition {
    private static final boolean HAS_CAN_USE_SLOT_METHOD = Skript.methodExists(LivingEntity.class, "canUseEquipmentSlot", EquipmentSlot.class);
    private static final boolean HAS_BODY_SLOT = Skript.fieldExists(EquipmentSlot.class, "BODY");
    private Expression<LivingEntity> entities;
    private Expression<ItemType> types;

    @Override
    public boolean init(Expression<?>[] vars, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = vars[0];
        this.types = vars[1];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        ItemType[] cachedTypes = this.types.getAll(event);
        return this.entities.check(event, entity -> {
            EntityEquipment equipment = entity.getEquipment();
            if (equipment == null) {
                return false;
            }
            ItemStack[] contents = (ItemStack[])Arrays.stream(EquipmentSlot.values()).filter(slot -> {
                if (HAS_CAN_USE_SLOT_METHOD) {
                    return entity.canUseEquipmentSlot(slot);
                }
                if (HAS_BODY_SLOT && slot == EquipmentSlot.BODY) {
                    return entity instanceof Horse || entity instanceof Wolf || entity instanceof Llama;
                }
                return true;
            }).map(arg_0 -> ((EntityEquipment)equipment).getItem(arg_0)).toArray(ItemStack[]::new);
            return SimpleExpression.check(cachedTypes, type -> {
                for (ItemStack content : contents) {
                    if (!(type.isOfType(content) ^ type.isAll())) continue;
                    return !type.isAll();
                }
                return type.isAll();
            }, false, false);
        }, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, this.entities, "wearing " + this.types.toString(event, debug));
    }

    static {
        PropertyCondition.register(CondIsWearing.class, "wearing %itemtypes%", "livingentities");
    }
}

