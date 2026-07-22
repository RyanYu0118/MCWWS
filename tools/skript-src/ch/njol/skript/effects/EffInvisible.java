/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Make Invisible")
@Description(value={"Makes a living entity visible/invisible. This is not a potion and therefore does not have features such as a time limit or particles.", "When setting an entity to invisible while using an invisibility potion on it, the potion will be overridden and when it runs out the entity keeps its invisibility."})
@Example(value="make target entity invisible")
@Since(value={"2.7"})
public class EffInvisible
extends Effect {
    private boolean invisible;
    private Expression<LivingEntity> livingEntities;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.livingEntities = exprs[0];
        this.invisible = matchedPattern == 0;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.livingEntities.getArray(event)) {
            entity.setInvisible(this.invisible);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.livingEntities.toString(event, debug) + " " + (this.invisible ? "in" : "") + "visible";
    }

    static {
        if (Skript.methodExists(LivingEntity.class, "isInvisible", new Class[0]) || Skript.methodExists(Entity.class, "isInvisible", new Class[0])) {
            Skript.registerEffect(EffInvisible.class, "make %livingentities% (invisible|not visible)", "make %livingentities% (visible|not invisible)");
        }
    }
}

