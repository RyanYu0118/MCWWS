/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.event.Event
 *  org.bukkit.util.Transformation
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3f
 */
package org.skriptlang.skript.bukkit.entity.displays.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Display;
import org.bukkit.event.Event;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Display Transformation Scale/Translation")
@Description(value={"Returns or changes the transformation scale or translation of <a href='#display'>displays</a>."})
@Example(value="set transformation translation of display to vector from -0.5, -0.5, -0.5 # Center the display in the same position as a block")
@Since(value={"2.10"})
public class ExprDisplayTransformationScaleTranslation
extends SimplePropertyExpression<Display, Vector> {
    private boolean scale;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprDisplayTransformationScaleTranslation.infoBuilder(ExprDisplayTransformationScaleTranslation.class, Vector.class, "(display|[display] transformation) (:scale|translation)", "displays", true).supplier(ExprDisplayTransformationScaleTranslation::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.scale = parseResult.hasTag("scale");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Vector convert(Display display) {
        Transformation transformation = display.getTransformation();
        return Vector.fromJOML((Vector3f)(this.scale ? transformation.getScale() : transformation.getTranslation()));
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.RESET -> CollectionUtils.array(Vector.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Vector3f vector = null;
        if (mode == Changer.ChangeMode.RESET) {
            Vector3f vector3f = vector = this.scale ? new Vector3f(1.0f, 1.0f, 1.0f) : new Vector3f(0.0f, 0.0f, 0.0f);
        }
        if (delta != null) {
            vector = ((Vector)delta[0]).toVector3f();
        }
        if (vector == null || !vector.isFinite()) {
            return;
        }
        for (Display display : (Display[])this.getExpr().getArray(event)) {
            Transformation transformation = display.getTransformation();
            Transformation change = this.scale ? new Transformation(transformation.getTranslation(), transformation.getLeftRotation(), vector, transformation.getRightRotation()) : new Transformation(vector, transformation.getLeftRotation(), transformation.getScale(), transformation.getRightRotation());
            display.setTransformation(change);
        }
    }

    @Override
    public Class<? extends Vector> getReturnType() {
        return Vector.class;
    }

    @Override
    protected String getPropertyName() {
        return "transformation " + (this.scale ? "scale" : "translation");
    }
}

