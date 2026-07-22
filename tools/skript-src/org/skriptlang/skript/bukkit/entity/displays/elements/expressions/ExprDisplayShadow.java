/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
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
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Display Shadow Radius/Strength")
@Description(value={"Returns or changes the shadow radius/strength of <a href='#display'>displays</a>."})
@Example(value="set shadow radius of the last spawned text display to 1.75")
@Since(value={"2.10"})
public class ExprDisplayShadow
extends SimplePropertyExpression<Display, Float> {
    private boolean radius;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprDisplayShadow.infoBuilder(ExprDisplayShadow.class, Float.class, "shadow (:radius|strength)", "displays", true).supplier(ExprDisplayShadow::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.radius = parseResult.hasTag("radius");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Float convert(Display display) {
        return Float.valueOf(this.radius ? display.getShadowRadius() : display.getShadowStrength());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Number.class);
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        float change;
        Display[] displays = (Display[])this.getExpr().getArray(event);
        float f = change = delta == null ? 0.0f : ((Number)delta[0]).floatValue();
        if (Float.isInfinite(change) || Float.isNaN(change)) {
            return;
        }
        switch (mode) {
            case REMOVE: {
                change = -change;
            }
            case ADD: {
                for (Display display : displays) {
                    float value;
                    if (this.radius) {
                        value = Math.max(0.0f, display.getShadowRadius() + change);
                        if (Float.isInfinite(value)) continue;
                        display.setShadowRadius(value);
                        continue;
                    }
                    value = Math.max(0.0f, display.getShadowStrength() + change);
                    if (Float.isInfinite(value)) continue;
                    display.setShadowStrength(value);
                }
                break;
            }
            case RESET: {
                if (!this.radius) {
                    change = 1.0f;
                }
            }
            case SET: {
                change = Math.max(0.0f, change);
                for (Display display : displays) {
                    if (this.radius) {
                        display.setShadowRadius(change);
                        continue;
                    }
                    display.setShadowStrength(change);
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Float> getReturnType() {
        return Float.class;
    }

    @Override
    protected String getPropertyName() {
        return this.radius ? "radius" : "strength";
    }
}

