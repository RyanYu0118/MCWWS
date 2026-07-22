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

@Name(value="Text Display Opacity")
@Description(value={"Returns or changes the text opacity of <a href='#display'>text displays</a>. The default is 255, fully opaque.\nValues are between 0 and 255. 0 to 3 are treated the same as 255, meaning fully opaque.\nValues from 4 to 26 are fully transparent, and opacity increases linearly from there up to 255.\nFor backwards compatability, setting negative values between -1 and -128 wrap around, so -1 is the same as 255 and -128 is the same as 128.\nAdding or subtracting values will adjust the opacity within the bounds of 0-255, so subtracting 300 wil always result in an opacity of 0.\n"})
@Example.Examples(value={@Example(value="set the text opacity of the last spawned text display to 0 # fully opaque"), @Example(value="set text opacity of all text displays to 255 # fully opaque"), @Example(value="set text opacity of all text displays to 128 # semi-transparent"), @Example(value="set text opacity of all text displays to 4 # fully transparent")})
@Since(value={"2.10, 2.14 (0-255)"})
public class ExprTextDisplayOpacity
extends SimplePropertyExpression<Display, Integer> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprTextDisplayOpacity.infoBuilder(ExprTextDisplayOpacity.class, Integer.class, "[display] [text] opacity", "displays", true).supplier(ExprTextDisplayOpacity::new)).build());
    }

    private static int convertToUnsigned(byte value) {
        return value < 0 ? 256 + value : value;
    }

    private static byte convertToSigned(int value) {
        if (value > 127) {
            value -= 256;
        }
        return (byte)value;
    }

    @Override
    @Nullable
    public Integer convert(Display display) {
        if (display instanceof TextDisplay) {
            TextDisplay textDisplay = (TextDisplay)display;
            return ExprTextDisplayOpacity.convertToUnsigned(textDisplay.getTextOpacity());
        }
        return null;
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
        Display[] displays = (Display[])this.getExpr().getArray(event);
        int change = delta == null ? 255 : ((Number)delta[0]).intValue();
        switch (mode) {
            case REMOVE: 
            case REMOVE_ALL: {
                change = -change;
            }
            case ADD: {
                for (Display display : displays) {
                    if (!(display instanceof TextDisplay)) continue;
                    TextDisplay textDisplay = (TextDisplay)display;
                    byte value = ExprTextDisplayOpacity.convertToSigned(Math.clamp((long)(ExprTextDisplayOpacity.convertToUnsigned(textDisplay.getTextOpacity()) + change), 0, 255));
                    textDisplay.setTextOpacity(value);
                }
                break;
            }
            case RESET: 
            case SET: 
            case DELETE: {
                change = ExprTextDisplayOpacity.convertToSigned(Math.clamp((long)change, -128, 255));
                for (Display display : displays) {
                    if (!(display instanceof TextDisplay)) continue;
                    TextDisplay textDisplay = (TextDisplay)display;
                    textDisplay.setTextOpacity((byte)change);
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
        return "opacity";
    }
}

