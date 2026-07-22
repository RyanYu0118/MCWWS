/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Strider
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
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Strider;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Strider Shivering")
@Description(value={"Make a strider start/stop shivering."})
@Example(value="if last spawned strider is shivering:\n\tmake last spawned strider stop shivering\n")
@Since(value={"2.12"})
public class EffStriderShivering
extends Effect {
    private Expression<LivingEntity> entities;
    private boolean start;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.start = matchedPattern <= 1;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Strider)) continue;
            Strider strider = (Strider)entity;
            strider.setShivering(this.start);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("make", this.entities);
        if (this.start) {
            builder.append((Object)"start");
        } else {
            builder.append((Object)"stop");
        }
        builder.append((Object)"shivering");
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffStriderShivering.class, "make %livingentities% start shivering", "force %livingentities% to start shivering", "make %livingentities% stop shivering", "force %livingentities% to stop shivering");
    }
}

