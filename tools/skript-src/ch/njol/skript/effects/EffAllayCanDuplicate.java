/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Allay
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
import org.bukkit.entity.Allay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Allay Duplicate")
@Description(value={"Set whether an allay can or cannot duplicate itself.", "This is not the same as breeding allays."})
@Example.Examples(value={@Example(value="allow all allays to duplicate"), @Example(value="prevent all allays from duplicating")})
@Since(value={"2.11"})
public class EffAllayCanDuplicate
extends Effect {
    private Expression<LivingEntity> entities;
    private boolean duplicate;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.duplicate = matchedPattern == 0;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Allay)) continue;
            Allay allay = (Allay)entity;
            allay.setCanDuplicate(this.duplicate);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.duplicate) {
            return "allow " + this.entities.toString(event, debug) + " to duplicate";
        }
        return "prevent " + this.entities.toString(event, debug) + " from duplicating";
    }

    static {
        Skript.registerEffect(EffAllayCanDuplicate.class, "allow %livingentities% to (duplicate|clone)", "prevent %livingentities% from (duplicating|cloning)");
    }
}

