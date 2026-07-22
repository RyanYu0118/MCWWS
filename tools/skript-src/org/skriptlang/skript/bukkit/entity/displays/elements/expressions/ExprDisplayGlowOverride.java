/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
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
import ch.njol.skript.util.ColorRGB;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Display Glow Color Override")
@Description(value={"Returns or changes the glowing color override of <a href='#display'>displays</a>.", "This overrides whatever color is already set for the scoreboard team of the displays."})
@Example(value="set glow color override of the last spawned text display to blue")
@Since(value={"2.10"})
public class ExprDisplayGlowOverride
extends SimplePropertyExpression<Display, ch.njol.skript.util.Color> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprDisplayGlowOverride.infoBuilder(ExprDisplayGlowOverride.class, ch.njol.skript.util.Color.class, "glow[ing] colo[u]r[s] override[s]", "displays", true).supplier(ExprDisplayGlowOverride::new)).build());
    }

    @Override
    @Nullable
    public ch.njol.skript.util.Color convert(Display display) {
        Color color = display.getGlowColorOverride();
        return color != null ? ColorRGB.fromBukkitColor(color) : null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE -> CollectionUtils.array(ch.njol.skript.util.Color.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Color color = delta != null ? ((ch.njol.skript.util.Color)delta[0]).asBukkitColor() : null;
        for (Display display : (Display[])this.getExpr().getArray(event)) {
            display.setGlowColorOverride(color);
        }
    }

    @Override
    public Class<? extends ch.njol.skript.util.Color> getReturnType() {
        return ch.njol.skript.util.Color.class;
    }

    @Override
    protected String getPropertyName() {
        return "glow color override";
    }
}

