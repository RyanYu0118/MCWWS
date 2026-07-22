/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Goat
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
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Goat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Goat Horns")
@Description(value={"Make a goat have or not have a left, right, or both horns."})
@Example.Examples(value={@Example(value="remove the left horn of last spawned goat"), @Example(value="regrow {_goat}'s horns"), @Example(value="remove both horns of all goats")})
@Since(value={"2.11"})
public class EffGoatHorns
extends Effect {
    private Expression<LivingEntity> entities;
    private GoatHorn goatHorn = GoatHorn.LEFT;
    private boolean remove;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (parseResult.hasTag("right")) {
            this.goatHorn = GoatHorn.RIGHT;
        } else if (parseResult.hasTag("both")) {
            this.goatHorn = GoatHorn.BOTH;
        }
        this.entities = exprs[0];
        this.remove = matchedPattern <= 1;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Goat)) continue;
            Goat goat = (Goat)entity;
            if (this.goatHorn != GoatHorn.RIGHT) {
                goat.setLeftHorn(this.remove);
            }
            if (this.goatHorn == GoatHorn.LEFT) continue;
            goat.setRightHorn(this.remove);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.remove) {
            builder.append((Object)"remove");
        } else {
            builder.append((Object)"regrow");
        }
        builder.append((Object)(switch (this.goatHorn.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> "the left horn";
            case 1 -> "the right horn";
            case 2 -> "both horns";
            case 3 -> "any horn";
        }));
        builder.append("of", this.entities);
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffGoatHorns.class, "remove [the] (left horn[s]|right:right horn[s]|both:both horns) of %livingentities%", "remove %livingentities%'[s] (left horn[s]|right:right horn[s]|both:horns)", "(regrow|replace) [the] (left horn[s]|right:right horn[s]|both:both horns) of %livingentities%", "(regrow|replace) %livingentities%'[s] (left horn[s]|right:right horn[s]|both:horns)");
    }

    public static enum GoatHorn {
        LEFT,
        RIGHT,
        BOTH,
        ANY;

    }
}

