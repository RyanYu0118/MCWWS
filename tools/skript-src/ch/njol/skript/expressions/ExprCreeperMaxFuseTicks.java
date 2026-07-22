/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Creeper
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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Creeper Max Fuse Ticks")
@Description(value={"The max fuse ticks that a creeper has."})
@Example(value="set target entity's max fuse ticks to 20 #1 second")
@Since(value={"2.5"})
public class ExprCreeperMaxFuseTicks
extends SimplePropertyExpression<LivingEntity, Long> {
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public Long convert(LivingEntity e) {
        return e instanceof Creeper ? (long)((Creeper)e).getMaxFuseTicks() : 0L;
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
        block8: for (LivingEntity le : (LivingEntity[])this.getExpr().getArray(e)) {
            if (!(le instanceof Creeper)) continue;
            Creeper c = (Creeper)le;
            switch (mode) {
                case ADD: {
                    int r1 = c.getMaxFuseTicks() + d;
                    if (r1 < 0) {
                        r1 = 0;
                    }
                    c.setMaxFuseTicks(r1);
                    continue block8;
                }
                case SET: {
                    c.setMaxFuseTicks(d);
                    continue block8;
                }
                case DELETE: {
                    c.setMaxFuseTicks(0);
                    continue block8;
                }
                case RESET: {
                    c.setMaxFuseTicks(30);
                    continue block8;
                }
                case REMOVE: {
                    int r2 = c.getMaxFuseTicks() - d;
                    if (r2 < 0) {
                        r2 = 0;
                    }
                    c.setMaxFuseTicks(r2);
                    continue block8;
                }
                case REMOVE_ALL: {
                    if (!$assertionsDisabled) {
                        throw new AssertionError();
                    }
                    continue block8;
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
        return "creeper max fuse ticks";
    }

    static {
        boolean bl = $assertionsDisabled = !ExprCreeperMaxFuseTicks.class.desiredAssertionStatus();
        if (Skript.methodExists(LivingEntity.class, "getMaxFuseTicks", new Class[0])) {
            ExprCreeperMaxFuseTicks.register(ExprCreeperMaxFuseTicks.class, Long.class, "[creeper] max[imum] fuse tick[s]", "livingentities");
        }
    }
}

