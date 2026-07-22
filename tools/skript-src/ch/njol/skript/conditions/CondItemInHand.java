/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.EntityEquipment
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.EntityEquipment;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;

@Name(value="Is Holding")
@Description(value={"Checks whether a player is holding a specific item. Cannot be used with endermen, use 'entity is [not] an enderman holding &lt;item type&gt;' instead."})
@Example.Examples(value={@Example(value="player is holding a stick"), @Example(value="victim isn't holding a diamond sword of sharpness")})
@Since(value={"1.0"})
public class CondItemInHand
extends Condition {
    private Expression<LivingEntity> entities;
    private Expression<ItemType> items;
    private boolean offTool;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.items = exprs[1];
        this.offTool = matchedPattern == 2 || matchedPattern == 3 || matchedPattern == 6 || matchedPattern == 7;
        this.setNegated(matchedPattern >= 4);
        return true;
    }

    @Override
    public boolean check(Event e) {
        return this.entities.check(e, livingEntity -> this.items.check(e, itemType -> {
            EntityEquipment equipment = livingEntity.getEquipment();
            if (equipment == null) {
                return false;
            }
            ItemType handItem = new ItemType(this.offTool ? equipment.getItemInOffHand() : equipment.getItemInMainHand());
            return Comparators.compare(handItem, itemType).isImpliedBy(Relation.EQUAL);
        }), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.entities.toString(e, debug) + " " + (this.entities.isSingle() ? "is" : "are") + " holding " + this.items.toString(e, debug) + (this.offTool ? " in off-hand" : "");
    }

    static {
        Skript.registerCondition(CondItemInHand.class, "[%livingentities%] ha(s|ve) %itemtypes% in [main] hand", "[%livingentities%] (is|are) holding %itemtypes% [in main hand]", "[%livingentities%] ha(s|ve) %itemtypes% in off[(-| )]hand", "[%livingentities%] (is|are) holding %itemtypes% in off[(-| )]hand", "[%livingentities%] (ha(s|ve) not|do[es]n't have) %itemtypes% in [main] hand", "[%livingentities%] (is not|isn't) holding %itemtypes% [in main hand]", "[%livingentities%] (ha(s|ve) not|do[es]n't have) %itemtypes% in off[(-| )]hand", "[%livingentities%] (is not|isn't) holding %itemtypes% in off[(-| )]hand");
    }
}

