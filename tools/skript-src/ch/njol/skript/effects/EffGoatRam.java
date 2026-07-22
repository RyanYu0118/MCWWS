/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Goat
 *  org.bukkit.entity.LivingEntity
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
import ch.njol.util.Kleenean;
import org.bukkit.entity.Goat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Make Goat Ram")
@Description(value={"Make a goat ram an entity.", "Ramming does have a cooldown and currently no way to change it."})
@Example(value="make all goats ram player")
@Since(value={"2.11"})
public class EffGoatRam
extends Effect {
    private Expression<LivingEntity> entities;
    private Expression<LivingEntity> target;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.target = exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        LivingEntity target = this.target.getSingle(event);
        if (target == null) {
            return;
        }
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Goat)) continue;
            Goat goat = (Goat)entity;
            goat.ram(target);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.entities.toString(event, debug) + " ram " + this.target.toString(event, debug);
    }

    static {
        if (Skript.methodExists(Goat.class, "ram", LivingEntity.class)) {
            Skript.registerEffect(EffGoatRam.class, "make %livingentities% ram %livingentity%", "force %livingentities% to ram %livingentity%");
        }
    }
}

