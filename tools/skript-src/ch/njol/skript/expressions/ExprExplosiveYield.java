/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Creeper
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Explosive
 *  org.bukkit.entity.Ghast
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Ghast;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Explosive Yield")
@Description(value={"The yield of an explosive (creeper, ghast, primed tnt, fireball, etc.). This is how big of an explosion is caused by the entity.", "Read <a href='https://minecraft.wiki/w/Explosion'>this wiki page</a> for more information.", "The yield of ghasts can only be set to between 0 and 127."})
@Example(value="on spawn of a creeper:\n\tset the explosive yield of the event-entity to 10\n")
@Since(value={"2.5, 2.11 (ghasts)"})
public class ExprExplosiveYield
extends SimplePropertyExpression<Entity, Number> {
    private static final boolean SUPPORTS_GHASTS = Skript.methodExists(Ghast.class, "getExplosionPower", new Class[0]);

    @Override
    @Nullable
    public Number convert(Entity entity) {
        if (entity instanceof Explosive) {
            Explosive explosive = (Explosive)entity;
            return Float.valueOf(explosive.getYield());
        }
        if (entity instanceof Creeper) {
            Creeper creeper = (Creeper)entity;
            return creeper.getExplosionRadius();
        }
        if (SUPPORTS_GHASTS && entity instanceof Ghast) {
            Ghast ghast = (Ghast)entity;
            return ghast.getExplosionPower();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Number.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Integer number = delta != null ? (Number)((Number)delta[0]) : (Number)0;
        float floatValue = Math2.fit(0.0f, ((Number)number).floatValue(), Float.MAX_VALUE);
        int intValue = Math2.fit(0, number, Integer.MAX_VALUE);
        for (Entity entity : (Entity[])this.getExpr().getArray(event)) {
            if (entity instanceof Explosive) {
                Explosive explosive = (Explosive)entity;
                this.changeExplosion(mode, floatValue, () -> ((Explosive)explosive).getYield(), arg_0 -> ((Explosive)explosive).setYield(arg_0), Float.MAX_VALUE);
                continue;
            }
            if (entity instanceof Creeper) {
                Creeper creeper = (Creeper)entity;
                this.changeExplosion(mode, intValue, () -> ((Creeper)creeper).getExplosionRadius(), arg_0 -> ((Creeper)creeper).setExplosionRadius(arg_0), Integer.MAX_VALUE);
                continue;
            }
            if (!SUPPORTS_GHASTS || !(entity instanceof Ghast)) continue;
            Ghast ghast = (Ghast)entity;
            this.changeExplosion(mode, intValue, () -> ((Ghast)ghast).getExplosionPower(), arg_0 -> ((Ghast)ghast).setExplosionPower(arg_0), 127);
        }
    }

    private void changeExplosion(Changer.ChangeMode mode, float value, Supplier<Float> getter, Consumer<Float> setter, float max) {
        setter.accept(Float.valueOf(Math2.fit(0.0f, switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE -> value;
            case Changer.ChangeMode.ADD -> getter.get().floatValue() + value;
            case Changer.ChangeMode.REMOVE -> getter.get().floatValue() - value;
            default -> throw new IllegalArgumentException("Unexpected mode: " + String.valueOf((Object)mode));
        }, max)));
    }

    private void changeExplosion(Changer.ChangeMode mode, int value, Supplier<Integer> getter, Consumer<Integer> setter, int max) {
        setter.accept(Math2.fit(0, switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE -> value;
            case Changer.ChangeMode.ADD -> getter.get() + value;
            case Changer.ChangeMode.REMOVE -> getter.get() - value;
            default -> throw new IllegalArgumentException("Unexpected mode: " + String.valueOf((Object)mode));
        }, max));
    }

    @Override
    public Class<Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "explosive yield";
    }

    static {
        ExprExplosiveYield.register(ExprExplosiveYield.class, Number.class, "explosive (yield|radius|size|power)", "entities");
    }
}

