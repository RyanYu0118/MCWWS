/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Panda
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
import org.bukkit.entity.Panda;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Force Panda On Back")
@Description(value={"Make a panda get on/off its back."})
@Example(value="if last spawned panda is on its back:\n\tmake last spawned panda get off its back\n")
@Since(value={"2.11"})
public class EffPandaOnBack
extends Effect {
    private Expression<LivingEntity> entities;
    private boolean getOn;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.getOn = parseResult.hasTag("on");
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Panda)) continue;
            Panda panda = (Panda)entity;
            panda.setOnBack(this.getOn);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("make", this.entities, "get");
        if (this.getOn) {
            builder.append((Object)"on");
        } else {
            builder.append((Object)"off");
        }
        builder.append((Object)"their backs");
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffPandaOnBack.class, "make %livingentities% get (:on|off) (its|their) back[s]", "force %livingentities% to get (:on|off) (its|their) back[s]");
    }
}

