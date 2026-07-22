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

@Name(value="Silence Entity")
@Description(value={"Controls whether or not an entity is silent."})
@Example(value="make target entity silent")
@Since(value={"2.5"})
public class EffSilence
extends Effect {
    private Expression<Entity> entities;
    private boolean silence;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.silence = matchedPattern % 2 == 0;
        return true;
    }

    @Override
    protected void execute(Event e) {
        for (Entity entity : this.entities.getArray(e)) {
            entity.setSilent(this.silence);
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (this.silence ? "silence " : "unsilence ") + this.entities.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffSilence.class, "silence %entities%", "unsilence %entities%", "make %entities% silent", "make %entities% not silent");
    }
}

