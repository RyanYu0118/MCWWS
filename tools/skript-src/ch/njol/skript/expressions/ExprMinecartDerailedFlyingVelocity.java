/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Minecart
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Minecart Derailed / Flying Velocity")
@Description(value={"The velocity of a minecart as soon as it has been derailed or as soon as it starts flying."})
@Example(value="on right click on minecart:\n\tset derailed velocity of event-entity to vector 2, 10, 2\n")
@Since(value={"2.5.1"})
public class ExprMinecartDerailedFlyingVelocity
extends SimplePropertyExpression<Entity, Vector> {
    private boolean flying;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.flying = parseResult.mark == 2;
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Vector convert(Entity entity) {
        if (entity instanceof Minecart) {
            Minecart mc = (Minecart)entity;
            return this.flying ? mc.getFlyingVelocityMod() : mc.getDerailedVelocityMod();
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case ADD: 
            case REMOVE: {
                return CollectionUtils.array(Vector.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        block18: {
            block19: {
                if (delta == null) break block18;
                if (!this.flying) break block19;
                switch (mode) {
                    case SET: {
                        for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                            if (!(entity instanceof Minecart)) continue;
                            ((Minecart)entity).setFlyingVelocityMod((Vector)delta[0]);
                        }
                        break block18;
                    }
                    case ADD: {
                        for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                            if (!(entity instanceof Minecart)) continue;
                            Minecart minecart = (Minecart)entity;
                            minecart.setFlyingVelocityMod(((Vector)delta[0]).add(minecart.getFlyingVelocityMod()));
                        }
                        break block18;
                    }
                    case REMOVE: {
                        for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                            if (!(entity instanceof Minecart)) continue;
                            Minecart minecart = (Minecart)entity;
                            minecart.setFlyingVelocityMod(((Vector)delta[0]).subtract(minecart.getFlyingVelocityMod()));
                        }
                        break block18;
                    }
                    default: {
                        assert (false);
                        break block18;
                    }
                }
            }
            switch (mode) {
                case SET: {
                    for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                        if (!(entity instanceof Minecart)) continue;
                        ((Minecart)entity).setDerailedVelocityMod((Vector)delta[0]);
                    }
                    break;
                }
                case ADD: {
                    for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                        if (!(entity instanceof Minecart)) continue;
                        Minecart minecart = (Minecart)entity;
                        minecart.setDerailedVelocityMod(((Vector)delta[0]).add(minecart.getDerailedVelocityMod()));
                    }
                    break;
                }
                case REMOVE: {
                    for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                        if (!(entity instanceof Minecart)) continue;
                        Minecart minecart = (Minecart)entity;
                        minecart.setDerailedVelocityMod(((Vector)delta[0]).subtract(minecart.getDerailedVelocityMod()));
                    }
                    break;
                }
                default: {
                    assert (false);
                    break;
                }
            }
        }
    }

    @Override
    protected String getPropertyName() {
        return (this.flying ? "flying" : "derailed") + " velocity";
    }

    @Override
    public Class<? extends Vector> getReturnType() {
        return Vector.class;
    }

    static {
        ExprMinecartDerailedFlyingVelocity.register(ExprMinecartDerailedFlyingVelocity.class, Vector.class, "[minecart] (1\u00a6derailed|2\u00a6flying) velocity", "entities");
    }
}

