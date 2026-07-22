/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Axolotl
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
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Play Dead")
@Description(value={"Make an axolotl start or stop playing dead."})
@Example(value="make last spawned axolotl play dead")
@Since(value={"2.11"})
public class EffPlayingDead
extends Effect {
    private Expression<LivingEntity> entities;
    private boolean playDead;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.playDead = matchedPattern <= 1;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Axolotl)) continue;
            Axolotl axolotl = (Axolotl)entity;
            axolotl.setPlayingDead(this.playDead);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.entities.toString(event, debug) + (this.playDead ? " start" : " stop") + " playing dead";
    }

    static {
        Skript.registerEffect(EffPlayingDead.class, "make %livingentities% (start playing|play) dead", "force %livingentities% to (start playing|play) dead", "make %livingentities% (stop playing|not play) dead", "force %livingentities% to (stop playing|not play) dead");
    }
}

