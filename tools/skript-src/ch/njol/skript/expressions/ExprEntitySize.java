/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Phantom
 *  org.bukkit.entity.Slime
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Slime;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity Size")
@Description(value={"Changes the entity size of slimes and phantoms. This is not the same as changing the scale attribute of an entity.", "When changing the size of a slime, its health is fully resorted and will have changes done to its max health, movement speed and attack damage.", "The default minecraft size of a slime is anywhere between 0 and 2, with a maximum of 126.", "The default minecraft size of a phantom is 0 with a maximum size of 64."})
@Example(value="spawn a slime at player:\n\tset entity size of event-entity to 5\n\tset name of event-entity to \"King Slime Jorg\"\n")
@Since(value={"2.11"})
public class ExprEntitySize
extends SimplePropertyExpression<LivingEntity, Integer> {
    private static final int MAXIMUM_SLIME_SIZE = 127;
    private static final int MAXIMUM_PHANTOM_SIZE = 64;

    @Override
    @Nullable
    public Integer convert(LivingEntity from) {
        if (from instanceof Phantom) {
            Phantom phantom = (Phantom)from;
            return phantom.getSize();
        }
        if (from instanceof Slime) {
            Slime slime = (Slime)from;
            return slime.getSize() - 1;
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.SET -> CollectionUtils.array(Number.class);
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int sizeDelta;
        double deltaSizeDouble;
        if (delta == null && mode != Changer.ChangeMode.RESET) {
            return;
        }
        double d = deltaSizeDouble = delta != null ? ((Number)delta[0]).doubleValue() : 0.0;
        if (Double.isNaN(deltaSizeDouble) || Double.isInfinite(deltaSizeDouble)) {
            return;
        }
        int n = sizeDelta = delta != null ? ((Number)delta[0]).intValue() : 0;
        if (mode == Changer.ChangeMode.REMOVE) {
            sizeDelta = -sizeDelta;
        }
        switch (mode) {
            case ADD: 
            case REMOVE: {
                for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
                    int newSize;
                    if (entity instanceof Phantom) {
                        Phantom phantom = (Phantom)entity;
                        newSize = Math2.fit(0, phantom.getSize() + sizeDelta, 64);
                        phantom.setSize(newSize);
                        continue;
                    }
                    if (!(entity instanceof Slime)) continue;
                    Slime slime = (Slime)entity;
                    newSize = Math2.fit(1, slime.getSize() + sizeDelta, 127);
                    slime.setSize(newSize);
                }
                break;
            }
            case SET: 
            case RESET: {
                for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
                    if (entity instanceof Phantom) {
                        Phantom phantom = (Phantom)entity;
                        phantom.setSize(Math2.fit(0, sizeDelta, 64));
                        continue;
                    }
                    if (!(entity instanceof Slime)) continue;
                    Slime slime = (Slime)entity;
                    slime.setSize(Math2.fit(1, sizeDelta + 1, 127));
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "entity size";
    }

    static {
        ExprEntitySize.register(ExprEntitySize.class, Integer.class, "entity size", "livingentities");
    }
}

