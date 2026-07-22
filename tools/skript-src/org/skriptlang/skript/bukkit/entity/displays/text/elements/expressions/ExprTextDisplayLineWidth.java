/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.TextDisplay
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.displays.text.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Text Display Line Width")
@Description(value={"Returns or changes the line width of <a href='#display'>text displays</a>. Default is 200."})
@Example(value="set the line width of the last spawned text display to 300")
@Since(value={"2.10"})
public class ExprTextDisplayLineWidth
extends SimplePropertyExpression<Display, Integer> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprTextDisplayLineWidth.infoBuilder(ExprTextDisplayLineWidth.class, Integer.class, "line width", "displays", true).supplier(ExprTextDisplayLineWidth::new)).build());
    }

    @Override
    @Nullable
    public Integer convert(Display display) {
        if (display instanceof TextDisplay) {
            TextDisplay textDisplay = (TextDisplay)display;
            return textDisplay.getLineWidth();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.SET -> CollectionUtils.array(Number.class);
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Display[] displays = (Display[])this.getExpr().getArray(event);
        int change = delta == null ? 200 : ((Number)delta[0]).intValue();
        switch (mode) {
            case REMOVE: {
                change = -change;
            }
            case ADD: {
                for (Display display : displays) {
                    if (!(display instanceof TextDisplay)) continue;
                    TextDisplay textDisplay = (TextDisplay)display;
                    int value = Math.max(0, textDisplay.getLineWidth() + change);
                    textDisplay.setLineWidth(value);
                }
                break;
            }
            case SET: 
            case RESET: {
                change = Math.max(0, change);
                for (Display display : displays) {
                    if (!(display instanceof TextDisplay)) continue;
                    TextDisplay textDisplay = (TextDisplay)display;
                    textDisplay.setLineWidth(change);
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "line width";
    }
}

