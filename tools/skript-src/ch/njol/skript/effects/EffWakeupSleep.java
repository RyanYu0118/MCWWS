/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Bat
 *  org.bukkit.entity.Fox
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Villager
 *  org.bukkit.event.Event
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
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Fox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Wake And Sleep")
@Description(value={"Make bats and foxes sleep or wake up.", "Make villagers sleep by providing a location of a bed.", "Make players sleep by providing a location of a bed. Using 'with force' will bypass \"nearby monsters\" ,the max distance, allowing players to sleep even if the bed is far away, and lets players sleep in the nether and end. Does not work if the location of the bed is not in the world the player is currently in.", "Using 'without spawn location update' will make players wake up without setting their spawn location to the bed."})
@Example.Examples(value={@Example(value="make {_fox} go to sleep"), @Example(value="make {_bat} stop sleeping"), @Example(value="make {_villager} start sleeping at location(0, 0, 0)"), @Example(value="make player go to sleep at location(0, 0, 0) with force"), @Example(value="make player wake up without spawn location update")})
@Since(value={"2.11"})
public class EffWakeupSleep
extends Effect {
    private Expression<LivingEntity> entities;
    @Nullable
    private Expression<Location> location;
    private boolean sleep;
    private boolean force;
    private boolean setSpawn;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.sleep = matchedPattern <= 3;
        this.force = parseResult.hasTag("force");
        boolean bl = this.setSpawn = !parseResult.hasTag("spawn");
        if (this.sleep && exprs[1] != null) {
            if (exprs[2] == null) {
                return false;
            }
            this.location = Direction.combine(exprs[1], exprs[2]);
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Location location = null;
        if (this.location != null) {
            location = this.location.getSingle(event);
        }
        boolean failed = false;
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (entity instanceof Bat) {
                Bat bat = (Bat)entity;
                bat.setAwake(!this.sleep);
                continue;
            }
            if (entity instanceof Villager) {
                Villager villager = (Villager)entity;
                if (this.sleep && location == null) {
                    failed = true;
                    continue;
                }
                if (!this.sleep) {
                    villager.wakeup();
                    continue;
                }
                villager.sleep(location);
                continue;
            }
            if (entity instanceof Fox) {
                Fox fox = (Fox)entity;
                fox.setSleeping(this.sleep);
                continue;
            }
            if (!(entity instanceof HumanEntity)) continue;
            HumanEntity humanEntity = (HumanEntity)entity;
            if (this.sleep && location == null) {
                failed = true;
                continue;
            }
            if (!this.sleep) {
                humanEntity.wakeup(this.setSpawn);
                continue;
            }
            humanEntity.sleep(location, this.force);
        }
        if (failed) {
            this.warning("The provided location is not set. This effect will have no effect for villagers and players.");
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("make", this.entities);
        if (this.sleep) {
            builder.append((Object)"start");
        } else {
            builder.append((Object)"stop");
        }
        builder.append((Object)"sleeping");
        if (this.location != null) {
            builder.append((Object)this.location);
        }
        if (this.force) {
            builder.append((Object)"with force");
        }
        if (!this.setSpawn) {
            builder.append((Object)"without spawn location update");
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffWakeupSleep.class, "make %livingentities% (start sleeping|[go to] sleep) [%-direction% %-location%]", "force %livingentities% to (start sleeping|[go to] sleep) [%-direction% %-location%]", "make %players% (start sleeping|[go to] sleep) %direction% %location% (force:with force)", "force %players% to (start sleeping|[go to] sleep) %direction% %location% (force:with force)", "make %livingentities% (stop sleeping|wake up)", "force %livingentities% to (stop sleeping|wake up)", "make %players% (stop sleeping|wake up) (spawn:without spawn [location] update)", "force %players% to (stop sleeping|wake up) (spawn:without spawn [location] update)");
    }
}

