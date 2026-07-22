/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
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
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Lightning")
@Description(value={"Strike lightning at a given location. Can use 'lightning effect' to create a lightning that does not harm entities or start fires."})
@Example.Examples(value={@Example(value="strike lightning at the player"), @Example(value="strike lightning effect at the victim")})
@Since(value={"1.4"})
public class EffLightning
extends Effect {
    private Expression<Location> locations;
    private boolean effectOnly;
    @Nullable
    public static Entity lastSpawned;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.locations = Direction.combine(exprs[0], exprs[1]);
        this.effectOnly = parseResult.mark == 1;
        return true;
    }

    @Override
    protected void execute(Event e) {
        for (Location l : this.locations.getArray(e)) {
            lastSpawned = this.effectOnly ? l.getWorld().strikeLightningEffect(l) : l.getWorld().strikeLightning(l);
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "strike lightning " + (this.effectOnly ? "effect " : "") + this.locations.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffLightning.class, "(create|strike) lightning(1\u00a6[ ]effect|) %directions% %locations%");
        lastSpawned = null;
    }
}

