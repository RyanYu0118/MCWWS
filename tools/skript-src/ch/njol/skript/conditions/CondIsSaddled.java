/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.AbstractHorse
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Steerable
 *  org.bukkit.inventory.ItemStack
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Steerable;
import org.bukkit.inventory.ItemStack;

@Name(value="Is Saddled")
@Description(value={"Checks whether a given entity (horse or steerable) is saddled.", "If 'properly' is used, this will only return true if the entity is wearing specifically a saddle item."})
@Example(value="send whether {_horse} is saddled")
@Since(value={"2.10"})
public class CondIsSaddled
extends PropertyCondition<LivingEntity> {
    private boolean properly;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.properly = parseResult.hasTag("properly");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(LivingEntity entity) {
        if (entity instanceof Steerable) {
            Steerable steerable = (Steerable)entity;
            return steerable.hasSaddle();
        }
        if (entity instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse)entity;
            ItemStack saddle = horse.getInventory().getSaddle();
            return this.properly ? saddle != null && saddle.equals((Object)new ItemStack(Material.SADDLE)) : saddle != null;
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return this.properly ? "properly saddled" : "saddled";
    }

    static {
        CondIsSaddled.register(CondIsSaddled.class, "[:properly] saddled", "livingentities");
    }
}

