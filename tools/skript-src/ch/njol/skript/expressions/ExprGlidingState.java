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
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Gliding State")
@Description(value={"Sets of gets gliding state of player. It allows you to set gliding state of entity even if they do not have an <a href=\"https://minecraft.wiki/w/Elytra\">Elytra</a> equipped."})
@Example(value="set gliding of player to off")
@Since(value={"2.2-dev21"})
public class ExprGlidingState
extends SimplePropertyExpression<LivingEntity, Boolean> {
    @Override
    public Boolean convert(LivingEntity e) {
        return e.isGliding();
    }

    @Override
    protected String getPropertyName() {
        return "gliding state";
    }

    @Override
    public Class<Boolean> getReturnType() {
        return Boolean.class;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
            return new Class[]{Boolean.class};
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(e)) {
            entity.setGliding(delta == null ? false : (Boolean)delta[0]);
        }
    }

    static {
        ExprGlidingState.register(ExprGlidingState.class, Boolean.class, "(gliding|glider) [state]", "livingentities");
    }
}

