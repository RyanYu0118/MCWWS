/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Handedness")
@Description(value={"Make mobs left or right-handed. This does not affect players."})
@Example.Examples(value={@Example(value="spawn skeleton at spawn of world \"world\":\n\tmake entity left handed\n"), @Example(value="make all zombies in radius 10 of player right handed")})
@Since(value={"2.8.0"})
public class EffHandedness
extends Effect {
    private boolean leftHanded;
    private Expression<LivingEntity> livingEntities;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.leftHanded = parseResult.hasTag("left");
        this.livingEntities = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity livingEntity : this.livingEntities.getArray(event)) {
            if (!(livingEntity instanceof Mob)) continue;
            ((Mob)livingEntity).setLeftHanded(this.leftHanded);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.livingEntities.toString(event, debug) + " " + (this.leftHanded ? "left" : "right") + " handed";
    }

    static {
        if (Skript.methodExists(Mob.class, "setLeftHanded", Boolean.TYPE)) {
            Skript.registerEffect(EffHandedness.class, "make %livingentities% (:left|right)( |-)handed");
        }
    }
}

