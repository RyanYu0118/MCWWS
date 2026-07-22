/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.TextDisplay
 *  org.bukkit.entity.TextDisplay$TextAlignment
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

@Name(value="Text Display Alignment")
@Description(value={"Returns or changes the <a href='#textalignment'>alignment</a> setting of <a href='#display'>text displays</a>."})
@Example(value="set text alignment of the last spawned text display to left aligned")
@Since(value={"2.10"})
public class ExprTextDisplayAlignment
extends SimplePropertyExpression<Display, TextDisplay.TextAlignment> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprTextDisplayAlignment.infoBuilder(ExprTextDisplayAlignment.class, TextDisplay.TextAlignment.class, "text alignment[s]", "displays", true).supplier(ExprTextDisplayAlignment::new)).build());
    }

    @Override
    @Nullable
    public TextDisplay.TextAlignment convert(Display display) {
        if (display instanceof TextDisplay) {
            TextDisplay textDisplay = (TextDisplay)display;
            return textDisplay.getAlignment();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            case Changer.ChangeMode.SET -> CollectionUtils.array(TextDisplay.TextAlignment.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        TextDisplay.TextAlignment alignment = mode == Changer.ChangeMode.RESET ? TextDisplay.TextAlignment.CENTER : (TextDisplay.TextAlignment)delta[0];
        for (Display display : (Display[])this.getExpr().getArray(event)) {
            if (!(display instanceof TextDisplay)) continue;
            TextDisplay textDisplay = (TextDisplay)display;
            textDisplay.setAlignment(alignment);
        }
    }

    @Override
    public Class<? extends TextDisplay.TextAlignment> getReturnType() {
        return TextDisplay.TextAlignment.class;
    }

    @Override
    protected String getPropertyName() {
        return "text alignment";
    }
}

