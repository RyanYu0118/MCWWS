/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Toggle Custom Name Visibility")
@Description(value={"Toggles the custom name visibility of an entity."})
@Example.Examples(value={@Example(value="show the custom name of event-entity"), @Example(value="hide target's display name")})
@Since(value={"2.10"})
public class EffCustomName
extends Effect {
    private boolean showCustomName;
    private Expression<Entity> entities;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.showCustomName = parseResult.hasTag("show");
        this.entities = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Entity entity : this.entities.getArray(event)) {
            entity.setCustomNameVisible(this.showCustomName);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.showCustomName ? "show" : "hide the custom name of " + this.entities.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffCustomName.class, "(:show|hide) [the] (custom|display)[ ]name of %entities%", "(:show|hide) %entities%'[s] (custom|display)[ ]name");
    }
}

