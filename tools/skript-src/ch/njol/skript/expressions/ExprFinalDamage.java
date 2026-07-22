/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.HealthUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Final Damage")
@Description(value={"How much damage is done in a damage event, considering all types of damage reduction. Can NOT be changed."})
@Example(value="send \"%final damage%\" to victim")
@Since(value={"2.2-dev19"})
@Events(value={"damage"})
public class ExprFinalDamage
extends SimpleExpression<Number>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityDamageEvent.class);
    }

    @Nullable
    protected Number[] get(Event e) {
        if (!(e instanceof EntityDamageEvent)) {
            return new Number[0];
        }
        return new Number[]{HealthUtils.getFinalDamage((EntityDamageEvent)e)};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        Skript.error("Final damage cannot be changed; try changing the 'damage'");
        return null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the final damage";
    }

    static {
        Skript.registerExpression(ExprFinalDamage.class, Number.class, ExpressionType.SIMPLE, "[the] final damage");
    }
}

