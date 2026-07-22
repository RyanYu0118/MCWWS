/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.entity.TextDisplay
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.misc.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentUtils;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Text Of")
@Description(value={"Returns or changes the <a href='#string'>text/string</a> of <a href='#display'>displays</a>.", "Note that currently you can only use Skript chat codes when running Paper."})
@Example(value="set text of the last spawned text display to \"example\"")
@Since(value={"2.10"})
public class ExprTextOf
extends SimplePropertyExpression<Object, Component> {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprTextOf.infoBuilder(ExprTextOf.class, Component.class, "text[s]", "displays", false).supplier(ExprTextOf::new)).build());
    }

    @Override
    @Nullable
    public Component convert(Object object) {
        if (object instanceof TextDisplay) {
            TextDisplay textDisplay = (TextDisplay)object;
            return textDisplay.text();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            case Changer.ChangeMode.SET -> CollectionUtils.array(Component[].class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Component component = delta == null ? null : TextComponentUtils.joinByNewLine((Component[])Arrays.copyOf(delta, delta.length, Component[].class));
        for (Object object : this.getExpr().getArray(event)) {
            if (!(object instanceof TextDisplay)) continue;
            TextDisplay textDisplay = (TextDisplay)object;
            textDisplay.text(component);
        }
    }

    @Override
    public Class<? extends Component> getReturnType() {
        return Component.class;
    }

    @Override
    protected String getPropertyName() {
        return "text";
    }
}

