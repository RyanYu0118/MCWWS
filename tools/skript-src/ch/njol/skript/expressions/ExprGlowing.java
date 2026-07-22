/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
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
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Glowing")
@Description(value={"Indicates if targeted entity is glowing (new 1.9 effect) or not. Glowing entities can be seen through walls."})
@Example(value="set glowing of player to true")
@Since(value={"2.2-dev18"})
public class ExprGlowing
extends SimplePropertyExpression<Entity, Boolean> {
    @Override
    public Boolean convert(Entity e) {
        return e.isGlowing();
    }

    @Override
    protected String getPropertyName() {
        return "glowing";
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
        for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
            entity.setGlowing(delta == null ? false : (Boolean)delta[0]);
        }
    }

    static {
        ExprGlowing.register(ExprGlowing.class, Boolean.class, "glowing", "entities");
    }
}

