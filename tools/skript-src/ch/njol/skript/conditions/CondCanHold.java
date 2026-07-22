/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
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
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Can Hold")
@Description(value={"Tests whether a player or a chest can hold the given item."})
@Example.Examples(value={@Example(value="block can hold 200 cobblestone"), @Example(value="player has enough space for 64 feathers")})
@Since(value={"1.0"})
public class CondCanHold
extends Condition {
    private Expression<Inventory> invis;
    private Expression<ItemType> items;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.invis = exprs[0];
        this.items = exprs[1];
        if (this.items instanceof Literal) {
            for (ItemType t : (ItemType[])((Literal)this.items).getAll()) {
                if ((t = t.getItem()).isAll() || t.getTypes().size() == 1) continue;
                Skript.error("The condition 'can hold' can currently only be used with aliases that start with 'every' or 'all', or only represent one item.", ErrorQuality.SEMANTIC_ERROR);
                return false;
            }
        }
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        return this.invis.check(e, invi -> {
            if (!this.items.getAnd()) {
                return this.items.check(e, t -> t.getItem().hasSpace((Inventory)invi));
            }
            ItemStack[] buf = ItemType.getStorageContents(invi);
            return this.items.check(e, t -> t.getItem().addTo(buf));
        }, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.CAN, e, debug, this.invis, "hold " + this.items.toString(e, debug));
    }

    static {
        Skript.registerCondition(CondCanHold.class, "%inventories% (can hold|ha(s|ve) [enough] space (for|to hold)) %itemtypes%", "%inventories% (can(no|')t hold|(ha(s|ve) not|ha(s|ve)n't|do[es]n't have) [enough] space (for|to hold)) %itemtypes%");
    }
}

