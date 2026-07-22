/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Egg
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import org.bukkit.entity.Egg;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="The Egg")
@Description(value={"The egg thrown in a Player Egg Throw event."})
@Example(value="spawn an egg at the egg")
@Events(value={"Egg Throw"})
@Since(value={"2.7"})
public class ExprEgg
extends EventValueExpression<Egg> {
    public ExprEgg() {
        super(Egg.class, true);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the egg";
    }

    static {
        ExprEgg.register(ExprEgg.class, Egg.class, "[thrown] egg");
    }
}

