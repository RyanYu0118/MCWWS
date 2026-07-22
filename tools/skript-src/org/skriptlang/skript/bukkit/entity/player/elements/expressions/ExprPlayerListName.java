/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.player.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Player List Name")
@Description(value={"The name of a player in the player list in the tab menu."})
@Example(value="on join:\n\tplayer has permission \"name.red\"\n\tset the player's tab list name to \"<red>%player's name%\"\n")
@Since(value={"Before 2.1"})
@Keywords(value={"tablist", "tab list"})
public class ExprPlayerListName
extends SimplePropertyExpression<Player, Component> {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprPlayerListName.infoBuilder(ExprPlayerListName.class, Component.class, "(player|tab)[ ]list name[s]", "players", false).supplier(ExprPlayerListName::new)).build());
    }

    @Override
    public Component convert(Player player) {
        return player.playerListName();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(Component.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        TextComponent name = delta == null ? (mode == Changer.ChangeMode.RESET ? null : Component.empty()) : (Component)delta[0];
        for (Player player : (Player[])this.getExpr().getArray(event)) {
            player.playerListName((Component)name);
        }
    }

    @Override
    public Class<Component> getReturnType() {
        return Component.class;
    }

    @Override
    protected String getPropertyName() {
        return "tablist name";
    }
}

