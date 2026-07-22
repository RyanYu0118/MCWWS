/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityTransformEvent$TransformReason
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ExpressionType;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTransformEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Transform Reason")
@Description(value={"The <a href='#transformreason'>transform reason</a> within an entity <a href='#entity transform'>entity transform</a> event."})
@Example(value="on entity transform:\n\ttransform reason is infection, drowned or frozen\n")
@Since(value={"2.8.0"})
public class ExprTransformReason
extends EventValueExpression<EntityTransformEvent.TransformReason> {
    public ExprTransformReason() {
        super(EntityTransformEvent.TransformReason.class);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "transform reason";
    }

    static {
        Skript.registerExpression(ExprTransformReason.class, EntityTransformEvent.TransformReason.class, ExpressionType.SIMPLE, "[the] transform[ing] (cause|reason|type)");
    }
}

