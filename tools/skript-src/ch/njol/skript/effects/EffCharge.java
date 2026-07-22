/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Creeper
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.WitherSkull
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
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Charge Entity")
@Description(value={"Charges or uncharges a creeper or wither skull. A creeper is charged when it has been struck by lightning."})
@Example(value="on spawn of creeper:\n\tcharge the event-entity\n")
@Since(value={"2.5, 2.10 (wither skulls)"})
public class EffCharge
extends Effect {
    private Expression<Entity> entities;
    private boolean charge;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.charge = !parseResult.hasTag("un");
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Entity entity : this.entities.getArray(event)) {
            if (entity instanceof Creeper) {
                Creeper creeper = (Creeper)entity;
                creeper.setPowered(this.charge);
                continue;
            }
            if (!(entity instanceof WitherSkull)) continue;
            WitherSkull witherSkull = (WitherSkull)entity;
            witherSkull.setCharged(this.charge);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.entities.toString(event, debug) + (this.charge ? " charged" : " not charged");
    }

    static {
        Skript.registerEffect(EffCharge.class, "make %entities% [un:(un|not |non[-| ])](charged|powered)", "[:un](charge|power) %entities%");
    }
}

