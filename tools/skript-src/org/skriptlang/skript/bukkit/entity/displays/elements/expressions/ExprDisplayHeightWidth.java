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

@Name(value="Display Height/Width")
@Description(value={"Returns or changes the height or width of <a href='#display'>displays</a>.", "The rendering culling bounding box spans horizontally width/2 from entity position, which determines the point at which the display will be frustum culled (no longer rendered because the game determines you are no longer able to see it).", "If set to 0, no culling will occur on both the vertical and horizontal directions. Default is 0.0."})
@Example(value="set display height of the last spawned text display to 2.5")
@Since(value={"2.10"})
public class ExprDisplayHeightWidth
extends SimplePropertyExpression<Display, Float> {
    private boolean height;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprDisplayHeightWidth.infoBuilder(ExprDisplayHeightWidth.class, Float.class, "display (:height|width)", "displays", true).supplier(ExprDisplayHeightWidth::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.height = parseResult.hasTag("height");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Float convert(Display display) {
        return Float.valueOf(this.height ? display.getDisplayHeight() : display.getDisplayWidth());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET, Changer.ChangeMode.SET -> CollectionUtils.array(Number.class);
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
                    if (this.height) {
                        value = Math.max(0.0f, display.getDisplayHeight() + change);
                        if (Float.isInfinite(value)) continue;
                        display.setDisplayHeight(value);
                        continue;
                    }
                    value = Math.max(0.0f, display.getDisplayWidth() + change);
                    if (Float.isInfinite(value)) continue;
                    display.setDisplayWidth(value);
                }
                break;
            }
            case RESET: 
            case SET: {
                change = Math.max(0.0f, change);
                for (Display display : displays) {
                    if (this.height) {
                        display.setDisplayHeight(change);
                        continue;
                    }
                    display.setDisplayWidth(change);
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
        return this.height ? "height" : "width";
    }
}

