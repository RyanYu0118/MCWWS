/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityShootBowEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
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
import org.bukkit.event.entity.EntityShootBowEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Projectile Force")
@Description(value={"Returns the force at which a projectile was shot within an entity shoot bow event."})
@Example(value="on entity shoot projectile:\n\tset the velocity of shooter to vector(0,1,0) * projectile force\n")
@Since(value={"2.11"})
public class ExprProjectileForce
extends SimpleExpression<Float>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    protected Float @Nullable [] get(Event event) {
        if (!(event instanceof EntityShootBowEvent)) {
            return null;
        }
        EntityShootBowEvent shotBowEvent = (EntityShootBowEvent)event;
        return new Float[]{Float.valueOf(shotBowEvent.getForce())};
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityShootBowEvent.class);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Float> getReturnType() {
        return Float.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "projectile force";
    }

    static {
        Skript.registerExpression(ExprProjectileForce.class, Float.class, ExpressionType.SIMPLE, "[the] projectile force");
    }
}

