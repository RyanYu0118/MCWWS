/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.Display$Billboard
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

@Name(value="Display Billboard")
@Description(value={"Returns or changes the <a href='#billboard'>billboard</a> setting of <a href='#display'>displays</a>.", "This describes the axes/points around which the display can pivot.", "Displays spawn with the 'fixed' billboard by default. Resetting this expression will also set it to 'fixed'."})
@Example(value="set billboard of the last spawned text display to center")
@Since(value={"2.10"})
public class ExprDisplayBillboard
extends SimplePropertyExpression<Display, Display.Billboard> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprDisplayBillboard.infoBuilder(ExprDisplayBillboard.class, Display.Billboard.class, "bill[ |-]board[ing] [setting]", "displays", true).supplier(ExprDisplayBillboard::new)).build());
    }

    @Override
    @Nullable
    public Display.Billboard convert(Display display) {
        return display.getBillboard();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            case Changer.ChangeMode.SET -> CollectionUtils.array(Display.Billboard.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Display.Billboard billboard = delta != null ? (Display.Billboard)delta[0] : Display.Billboard.FIXED;
        for (Display display : (Display[])this.getExpr().getArray(event)) {
            display.setBillboard(billboard);
        }
    }

    @Override
    public Class<? extends Display.Billboard> getReturnType() {
        return Display.Billboard.class;
    }

    @Override
    protected String getPropertyName() {
        return "billboard";
    }
}

