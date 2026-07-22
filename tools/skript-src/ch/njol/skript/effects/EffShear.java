/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.entity.Shearable
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Sheep
 *  org.bukkit.entity.Snowman
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
import io.papermc.paper.entity.Shearable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Snowman;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Shear")
@Description(value={"Shears or un-shears a shearable entity with drops by shearing and a 'sheared' sound. Using with 'force' will force this effect despite the entity's 'shear state'.", "\nPlease note that..:", "\n- Force-shearing or un-shearing on a sheared mushroom cow is not possible"})
@Example(value="on rightclick on a sheep holding a sword:\n\tshear the clicked sheep\n\tchance of 10%\n\tforce shear the clicked sheep\n")
@Since(value={"2.0 (cows, sheep & snowmen), 2.8.0 (all shearable entities)"})
public class EffShear
extends Effect {
    private static final boolean INTERFACE_METHOD = Skript.classExists("io.papermc.paper.entity.Shearable");
    private Expression<LivingEntity> entity;
    private boolean force;
    private boolean shear;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entity = exprs[0];
        this.force = parseResult.hasTag("force");
        this.shear = matchedPattern == 0;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entity.getArray(event)) {
            if (this.shear && INTERFACE_METHOD) {
                if (!(entity instanceof Shearable)) continue;
                Shearable shearable = (Shearable)entity;
                if (!this.force && !shearable.readyToBeSheared()) continue;
                shearable.shear();
                continue;
            }
            if (entity instanceof Sheep) {
                ((Sheep)entity).setSheared(this.shear);
                continue;
            }
            if (!(entity instanceof Snowman)) continue;
            ((Snowman)entity).setDerp(this.shear);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.shear ? "" : "un") + "shear " + this.entity.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffShear.class, (INTERFACE_METHOD ? "[:force] " : "") + "shear %livingentities%", "un[-]shear %livingentities%");
    }
}

