/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Warden
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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Warden;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Warden Most Angered At")
@Description(value={"The entity a warden is most angry at.", "A warden can be angry towards multiple entities with different anger levels."})
@Example(value="if the most angered entity of last spawned warden is not player:\n\tset the most angered entity of last spawned warden to player\n")
@Since(value={"2.11"})
public class ExprWardenAngryAt
extends SimplePropertyExpression<LivingEntity, LivingEntity> {
    @Override
    @Nullable
    public LivingEntity convert(LivingEntity livingEntity) {
        if (!(livingEntity instanceof Warden)) {
            return null;
        }
        Warden warden = (Warden)livingEntity;
        return warden.getEntityAngryAt();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(LivingEntity.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (delta != null);
        LivingEntity target = (LivingEntity)delta[0];
        for (LivingEntity livingEntity : (LivingEntity[])this.getExpr().getArray(event)) {
            if (!(livingEntity instanceof Warden)) continue;
            Warden warden = (Warden)livingEntity;
            warden.setAnger((Entity)target, 150);
        }
    }

    @Override
    public Class<LivingEntity> getReturnType() {
        return LivingEntity.class;
    }

    @Override
    protected String getPropertyName() {
        return "most angered entity";
    }

    static {
        ExprWardenAngryAt.register(ExprWardenAngryAt.class, LivingEntity.class, "most angered entity", "livingentities");
    }
}

