/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
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
import ch.njol.skript.lang.ExpressionType;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Arrows Stuck")
@Description(value={"The number of arrows stuck in a living entity."})
@Example(value="set arrows stuck in player to 5")
@Since(value={"2.5"})
public class ExprArrowsStuck
extends SimplePropertyExpression<LivingEntity, Long> {
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public Long convert(LivingEntity le) {
        return le.getArrowsStuck();
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE_ALL) {
            return null;
        }
        return CollectionUtils.array(Number.class);
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int d = delta == null ? 0 : ((Number)delta[0]).intValue();
        block7: for (LivingEntity le : (LivingEntity[])this.getExpr().getArray(e)) {
            switch (mode) {
                case ADD: {
                    int r1 = le.getArrowsStuck() + d;
                    if (r1 < 0) {
                        r1 = 0;
                    }
                    le.setArrowsStuck(r1);
                    continue block7;
                }
                case SET: {
                    le.setArrowsStuck(d);
                    continue block7;
                }
                case DELETE: 
                case RESET: {
                    le.setArrowsStuck(0);
                    continue block7;
                }
                case REMOVE: {
                    int r2 = le.getArrowsStuck() - d;
                    if (r2 < 0) {
                        r2 = 0;
                    }
                    le.setArrowsStuck(r2);
                    continue block7;
                }
                case REMOVE_ALL: {
                    if (!$assertionsDisabled) {
                        throw new AssertionError();
                    }
                    continue block7;
                }
            }
        }
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "arrows stuck";
    }

    static {
        boolean bl = $assertionsDisabled = !ExprArrowsStuck.class.desiredAssertionStatus();
        if (Skript.methodExists(LivingEntity.class, "getArrowsStuck", new Class[0])) {
            Skript.registerExpression(ExprArrowsStuck.class, Long.class, ExpressionType.PROPERTY, "[number of] arrow[s] stuck in %livingentities%");
        }
    }
}

