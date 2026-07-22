/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.data.DefaultComparators;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.comparator.Relation;

@Name(value="Is of Type")
@Description(value={"Checks whether an item or an entity is of the given type. This is mostly useful for variables, as you can use the general 'is' condition otherwise (e.g. 'victim is a creeper')."})
@Example.Examples(value={@Example(value="tool is of type {selected type}"), @Example(value="victim is of type {villager type}")})
@Since(value={"1.4"})
public class CondIsOfType
extends Condition {
    private Expression<?> what;
    private Expression<?> types;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.what = exprs[0];
        this.types = exprs[1];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        return this.what.check(event, o1 -> this.types.check(event, o2 -> {
            if (o2 instanceof ItemType && o1 instanceof ItemStack) {
                return ((ItemType)o2).isSupertypeOf(new ItemType((ItemStack)o1));
            }
            if (o2 instanceof EntityData && o1 instanceof Entity) {
                return ((EntityData)o2).isInstance((Entity)o1);
            }
            if (o2 instanceof ItemType && o1 instanceof Entity) {
                return Relation.EQUAL.isImpliedBy(DefaultComparators.entityItemComparator.compare(EntityData.fromEntity((Entity)o1), (ItemType)o2));
            }
            return false;
        }), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, this.what, "of " + (this.types.isSingle() ? "type " : "types ") + this.types.toString(event, debug));
    }

    static {
        PropertyCondition.register(CondIsOfType.class, "of type[s] %itemtypes/entitydatas%", "itemstacks/entities");
    }
}

