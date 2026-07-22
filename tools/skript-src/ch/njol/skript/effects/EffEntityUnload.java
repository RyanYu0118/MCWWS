/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity Despawn")
@Description(value={"Make a living entity despawn when the chunk they're located at is unloaded.", "Setting a custom name on a living entity automatically makes it not despawnable.", "More information on what and when entities despawn can be found at <a href=\"https://minecraft.wiki/w/Mob_spawning#Despawning\">reference</a>."})
@Example.Examples(value={@Example(value="make all entities not despawnable on chunk unload"), @Example(value="spawn zombie at location(0, 0, 0):\n\tforce event-entity to not despawn when far away\n")})
@Since(value={"2.11"})
public class EffEntityUnload
extends Effect {
    private Expression<LivingEntity> entities;
    private boolean despawn;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.despawn = matchedPattern != 2;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entities.getArray(event)) {
            entity.setRemoveWhenFarAway(this.despawn);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.despawn) {
            return "make " + this.entities.toString(event, debug) + " despawn on chunk unload";
        }
        return "prevent " + this.entities.toString(event, debug) + " from despawning on chunk unload";
    }

    static {
        Skript.registerEffect(EffEntityUnload.class, "make %livingentities% despawn[able] (on chunk unload|when far away)", "force %livingentities% to despawn (on chunk unload|when far away)", "prevent %livingentities% from despawning [on chunk unload|when far away]");
    }
}

