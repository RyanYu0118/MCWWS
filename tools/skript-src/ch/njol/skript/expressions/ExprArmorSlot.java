/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.AbstractHorse
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HappyGhast
 *  org.bukkit.entity.Horse
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Llama
 *  org.bukkit.entity.Pig
 *  org.bukkit.entity.Strider
 *  org.bukkit.entity.TraderLlama
 *  org.bukkit.entity.Wolf
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.EntityEquipment
 *  org.bukkit.inventory.EquipmentSlot
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.slot.EquipmentSlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Strider;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event;
import org.bukkit.inventory.EntityEquipment;
import org.jetbrains.annotations.Nullable;

@Name(value="Armor Slot")
@Description(value={"Equipment of living entities, i.e. the boots, leggings, chestplate or helmet.", "Body armor is a special slot that can only be used for:", "<ul>", "<li>Horses: Horse armour (doesn't work on zombie or skeleton horses)</li>", "<li>Wolves: Wolf Armor</li>", "<li>Llamas (regular or trader): Carpet</li>", "<li>Happy Ghasts: Harness</li>", "</ul>", "Saddle is a special slot that can only be used for: pigs, striders and horse types (horse, camel, llama, mule, donkey)."})
@Example.Examples(value={@Example(value="set chestplate of the player to a diamond chestplate"), @Example(value="helmet of player is neither tag values of tag \"paper:helmets\" nor air # player is wearing a block, e.g. from another plugin")})
@Keywords(value={"armor"})
@Since(value={"1.0, 2.8.0 (armor), 2.10 (body armor), 2.12 (saddle)", "2.12.1 (happy ghast)"})
public class ExprArmorSlot
extends PropertyExpression<LivingEntity, Slot> {
    private static final Set<Class<? extends Entity>> BODY_ENTITIES = new HashSet<Class<TraderLlama>>(Set.of(Horse.class, Llama.class, TraderLlama.class));
    private static final Set<Class<? extends Entity>> SADDLE_ENTITIES = Set.of(Pig.class, Strider.class, AbstractHorse.class);
    private static final boolean HAS_BODY_SLOT = Skript.fieldExists(org.bukkit.inventory.EquipmentSlot.class, "BODY");
    private static final boolean HAS_SADDLE_SLOT = Skript.fieldExists(org.bukkit.inventory.EquipmentSlot.class, "SADDLE");
    private static final org.bukkit.inventory.EquipmentSlot BODY_SLOT = HAS_BODY_SLOT ? org.bukkit.inventory.EquipmentSlot.valueOf((String)"BODY") : null;
    private static final org.bukkit.inventory.EquipmentSlot SADDLE_SLOT;
    @Nullable
    private Literal<org.bukkit.inventory.EquipmentSlot> slots;
    private boolean explicitSlot;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression<?> slots = exprs[0];
        Expression<?> expr = exprs[1];
        if (matchedPattern == 1) {
            slots = exprs[1];
            expr = exprs[0];
        }
        if (slots != null) {
            this.slots = (Literal)slots;
        }
        this.explicitSlot = !parseResult.hasTag("item");
        this.setExpr(expr);
        return true;
    }

    protected Slot @Nullable [] get(Event event, LivingEntity[] source) {
        if (this.slots == null) {
            return (Slot[])Arrays.stream(source).map(LivingEntity::getEquipment).flatMap(equipment -> {
                if (equipment == null) {
                    return null;
                }
                if (HAS_BODY_SLOT && this.canUseEquipmentSlot(equipment.getHolder(), org.bukkit.inventory.EquipmentSlot.BODY)) {
                    return Stream.of(new EquipmentSlot((EntityEquipment)equipment, org.bukkit.inventory.EquipmentSlot.BODY, this.explicitSlot));
                }
                return Stream.of(new EquipmentSlot((EntityEquipment)equipment, org.bukkit.inventory.EquipmentSlot.HEAD, this.explicitSlot), new EquipmentSlot((EntityEquipment)equipment, org.bukkit.inventory.EquipmentSlot.CHEST, this.explicitSlot), new EquipmentSlot((EntityEquipment)equipment, org.bukkit.inventory.EquipmentSlot.LEGS, this.explicitSlot), new EquipmentSlot((EntityEquipment)equipment, org.bukkit.inventory.EquipmentSlot.FEET, this.explicitSlot));
            }).toArray(Slot[]::new);
        }
        org.bukkit.inventory.EquipmentSlot[] equipmentSlots = (org.bukkit.inventory.EquipmentSlot[])this.slots.getArray(event);
        if (equipmentSlots.length == 0) {
            return new EquipmentSlot[0];
        }
        ArrayList<EquipmentSlot> slots = new ArrayList<EquipmentSlot>();
        for (LivingEntity entity : source) {
            EntityEquipment equipment2 = entity.getEquipment();
            if (equipment2 == null) continue;
            for (org.bukkit.inventory.EquipmentSlot equipmentSlot : equipmentSlots) {
                if (!this.canUseEquipmentSlot((Entity)entity, equipmentSlot)) continue;
                slots.add(new EquipmentSlot(equipment2, equipmentSlot, this.explicitSlot));
            }
        }
        return (Slot[])slots.toArray(EquipmentSlot[]::new);
    }

    private boolean canUseEquipmentSlot(Entity entity, org.bukkit.inventory.EquipmentSlot equipmentSlot) {
        Class entityClass = entity.getType().getEntityClass();
        Set<Class<? extends Entity>> entityClasses = null;
        if (HAS_BODY_SLOT && equipmentSlot == BODY_SLOT) {
            entityClasses = BODY_ENTITIES;
        } else if (HAS_SADDLE_SLOT && equipmentSlot == SADDLE_SLOT) {
            entityClasses = SADDLE_ENTITIES;
        }
        if (entityClasses != null) {
            if (entityClasses.contains(entityClass)) {
                return true;
            }
            for (Class<? extends Entity> type : entityClasses) {
                if (!type.isInstance(entity)) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean isSingle() {
        return this.slots != null && this.slots.isSingle();
    }

    @Override
    public Class<Slot> getReturnType() {
        return Slot.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)"the");
        if (this.slots != null) {
            builder.append((Object)this.slots);
        } else {
            builder.append((Object)"armor");
        }
        if (!this.explicitSlot) {
            builder.append((Object)"items");
        }
        builder.append("of", this.getExpr());
        return builder.toString();
    }

    static {
        org.bukkit.inventory.EquipmentSlot equipmentSlot = SADDLE_SLOT = HAS_SADDLE_SLOT ? org.bukkit.inventory.EquipmentSlot.valueOf((String)"SADDLE") : null;
        if (Material.getMaterial((String)"WOLF_ARMOR") != null) {
            BODY_ENTITIES.add(Wolf.class);
        }
        if (Skript.classExists("org.bukkit.entity.HappyGhast")) {
            BODY_ENTITIES.add(HappyGhast.class);
        }
        ExprArmorSlot.register(ExprArmorSlot.class, Slot.class, "(%-*equipmentslots%|[the] armo[u]r[s]) [item:item[s]]", "livingentities");
    }
}

