/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ExperienceOrb
 *  org.bukkit.entity.Item
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.sections.EffSecSpawn;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.Experience;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Drop")
@Description(value={"Drops one or more items."})
@Example(value="on death of creeper:\n\tdrop 1 TNT\n")
@Since(value={"1.0"})
public class EffDrop
extends Effect {
    @Nullable
    public static Entity lastSpawned;
    private Expression<?> drops;
    private Expression<Location> locations;
    private boolean useVelocity;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.drops = exprs[0];
        this.locations = Direction.combine(exprs[1], exprs[2]);
        this.useVelocity = parseResult.mark == 0;
        return true;
    }

    @Override
    public void execute(Event e) {
        ?[] os = this.drops.getArray(e);
        for (Location l : this.locations.getArray(e)) {
            Location itemDropLoc = l.clone().subtract(0.5, 0.5, 0.5);
            for (Object o : os) {
                if (o instanceof Experience) {
                    ExperienceOrb orb = (ExperienceOrb)l.getWorld().spawn(l, ExperienceOrb.class);
                    orb.setExperience(((Experience)o).getXP() + orb.getExperience());
                    EffSecSpawn.lastSpawned = orb;
                    continue;
                }
                if (o instanceof ItemStack) {
                    o = new ItemType((ItemStack)o);
                }
                for (ItemStack is : ((ItemType)o).getItem().getAll()) {
                    if (ItemUtils.isAir(is.getType()) || is.getAmount() <= 0) continue;
                    if (this.useVelocity) {
                        lastSpawned = l.getWorld().dropItemNaturally(itemDropLoc, is);
                        continue;
                    }
                    Item item = l.getWorld().dropItem(l, is);
                    item.teleport(l);
                    item.setVelocity(new Vector(0, 0, 0));
                    lastSpawned = item;
                }
            }
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "drop " + this.drops.toString(e, debug) + " " + this.locations.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffDrop.class, "drop %itemtypes/experiences% [%directions% %locations%] [(1\u00a6without velocity)]");
        lastSpawned = null;
    }
}

