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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Display;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Display View Range")
@Description(value={"Returns or changes the view range of <a href='#display'>displays</a>.", "Default value is 1.0. This value is then multiplied by 64 and the player's entity view distance setting to determine the actual range.", "For example, a player with 150% entity view distance will see a block display with a view range of 1.2 at 1.2 * 64 * 150% = 115.2 blocks away."})
@Example(value="set view range of the last spawned text display to 2.9")
@Since(value={"2.10"})
public class ExprDisplayViewRange
extends SimplePropertyExpression<Display, Float> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprDisplayViewRange.infoBuilder(ExprDisplayViewRange.class, Float.class, "[display] view (range|radius)", "displays", true).supplier(ExprDisplayViewRange::new)).build());
    }

    @Override
    @Nullable
    public Float convert(Display display) {
        return Float.valueOf(display.getViewRange());
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
        float f = change = delta == null ? 1.0f : ((Number)delta[0]).floatValue();
        if (Float.isNaN(change) || Float.isInfinite(change)) {
            return;
        }
        switch (mode) {
            case REMOVE: {
                change = -change;
            }
            case ADD: {
                for (Display display : displays) {
                    float value = Math.max(0.0f, display.getViewRange() + change);
                    if (Float.isInfinite(value)) continue;
                    display.setViewRange(value);
                }
                break;
            }
            case SET: 
            case RESET: {
                change = Math.max(0.0f, change);
                for (Display display : displays) {
                    display.setViewRange(change);
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
        return "view range";
    }
}

