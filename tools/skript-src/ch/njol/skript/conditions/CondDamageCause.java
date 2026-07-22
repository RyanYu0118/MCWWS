/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Damage Cause")
@Description(value={"Tests what kind of damage caused a <a href='#damage'>damage event</a>. Refer to the <a href='#damagecause'>Damage Cause</a> type for a list of all possible causes."})
@Example.Examples(value={@Example(value="# make players use their potions of fire resistance whenever they take any kind of fire damage\non damage:\n\tdamage was caused by lava, fire or burning\n\tvictim is a player\n\tvictim has a potion of fire resistance\n\tcancel event\n\tapply fire resistance to the victim for 30 seconds\n\tremove 1 potion of fire resistance from the victim\n"), @Example(value="# prevent mobs from dropping items under certain circumstances\non death:\n\tentity is not a player\n\tdamage wasn't caused by a block explosion, an attack, a projectile, a potion, fire, burning, thorns or poison\n\tclear drops\n")})
@Since(value={"2.0"})
public class CondDamageCause
extends Condition {
    private Expression<EntityDamageEvent.DamageCause> cause;
    private Expression<EntityDamageEvent.DamageCause> expected;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.cause = new EventValueExpression<EntityDamageEvent.DamageCause>(EntityDamageEvent.DamageCause.class);
        this.expected = exprs[0];
        this.setNegated(parseResult.mark == 1);
        return ((EventValueExpression)this.cause).init();
    }

    @Override
    public boolean check(Event e) {
        EntityDamageEvent.DamageCause cause = this.cause.getSingle(e);
        if (cause == null) {
            return false;
        }
        return this.expected.check(e, other -> cause == other, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "damage was" + (this.isNegated() ? " not" : "") + " caused by " + this.expected.toString(e, debug);
    }

    static {
        Skript.registerCondition(CondDamageCause.class, "[the] damage (was|is|has)(0\u00a6|1\u00a6n('|o)t) [been] (caused|done|made) by %damagecause%");
    }
}

