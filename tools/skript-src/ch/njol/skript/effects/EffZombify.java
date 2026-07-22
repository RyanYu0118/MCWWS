/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Villager
 *  org.bukkit.entity.ZombieVillager
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import java.util.function.Function;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Zombify Villager")
@Description(value={"Turn a villager into a zombie villager. Cure a zombie villager immediately or after specified amount of time.", "This effect removes the old entity and creates a new entity.", "Zombifying a villager stored in a variable will update the variable to the new zombie villager.", "Curing a zombie villager does not update the variable."})
@Example.Examples(value={@Example(value="zombify last spawned villager"), @Example(value="set {_villager} to last spawned villager\nzombify {_villager}\nif {_villager} is a zombie villager:\n\t# This will pass because '{_villager}' gets changed to the new zombie villager\n"), @Example(value="set {_villager} to last spawned villager\nzombify last spawned villager\nif {_villager} is a zombie villager:\n\t# This will fail because the variable was not provided when zombifying\n"), @Example(value="unzombify {_zombieVillager}"), @Example(value="unzombify {_zombieVillager} after 2 seconds")})
@Since(value={"2.11"})
public class EffZombify
extends Effect {
    private Expression<LivingEntity> entities;
    @Nullable
    private Expression<Timespan> timespan;
    private boolean zombify;
    private boolean changeInPlace = false;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        boolean bl = this.zombify = matchedPattern == 0;
        if (!this.zombify && exprs[1] != null) {
            this.timespan = exprs[1];
        }
        if (Changer.ChangerUtils.acceptsChange(this.entities, Changer.ChangeMode.SET, LivingEntity.class)) {
            this.changeInPlace = true;
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Timespan timespan;
        int ticks = 0;
        if (this.timespan != null && (timespan = this.timespan.getSingle(event)) != null) {
            ticks = (int)timespan.getAs(Timespan.TimePeriod.TICK);
        }
        int finalTicks = ticks;
        Function<LivingEntity, LivingEntity> changeFunction = entity -> {
            if (this.zombify && entity instanceof Villager) {
                Villager villager = (Villager)entity;
                return villager.zombify();
            }
            if (!this.zombify && entity instanceof ZombieVillager) {
                ZombieVillager zombieVillager = (ZombieVillager)entity;
                zombieVillager.setConversionTime(finalTicks);
            }
            return entity;
        };
        if (this.changeInPlace) {
            this.entities.changeInPlace(event, changeFunction);
        } else {
            for (LivingEntity entity2 : this.entities.getAll(event)) {
                changeFunction.apply(entity2);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.zombify) {
            builder.append((Object)"zombify");
        } else {
            builder.append((Object)"unzombify");
        }
        builder.append((Object)this.entities);
        if (this.timespan != null) {
            builder.append("after", this.timespan);
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffZombify.class, "zombify %livingentities%", "unzombify %livingentities% [(in|after) %-timespan%]");
    }
}

