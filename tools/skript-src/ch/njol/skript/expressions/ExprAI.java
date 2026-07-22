/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity AI")
@Description(value={"Returns whether an entity has AI."})
@Example(value="set artificial intelligence of target entity to false")
@Since(value={"2.5"})
public class ExprAI
extends SimplePropertyExpression<LivingEntity, Boolean> {
    @Override
    @Nullable
    public Boolean convert(LivingEntity entity) {
        return entity.hasAI();
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return mode == Changer.ChangeMode.SET ? CollectionUtils.array(Boolean.class) : null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null || delta[0] == null) {
            return;
        }
        boolean value = (Boolean)delta[0];
        for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
            entity.setAI(value);
        }
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }

    @Override
    protected String getPropertyName() {
        return "artificial intelligence";
    }

    static {
        ExprAI.register(ExprAI.class, Boolean.class, "(ai|artificial intelligence)", "livingentities");
    }
}

