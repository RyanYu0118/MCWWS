/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Nameable
 *  org.bukkit.command.CommandSender
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.types;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.base.EventValueExpression;
import net.kyori.adventure.text.Component;
import org.bukkit.Nameable;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

@ApiStatus.Internal
public class NameableClassInfo
extends ClassInfo<Nameable> {
    public NameableClassInfo() {
        super(Nameable.class, "nameable");
        this.user("nameables?").name("Nameable").description("A variety of Bukkit types that can have names, such as entities and some blocks.").since("2.13").defaultExpression(new EventValueExpression<Nameable>(Nameable.class)).after("entity", "commandsender", "block", "player").property(Property.NAME, "The name of the nameable, if it has one, as text. Use 'display name' if you need a changeable name.", Skript.instance(), ExpressionPropertyHandler.of(nameable -> {
            if (nameable instanceof CommandSender) {
                CommandSender sender = (CommandSender)nameable;
                return sender.name();
            }
            return nameable.customName();
        }, Component.class)).property(Property.DISPLAY_NAME, "The custom name of the nameable, if it has one, as text. Can be set or reset.", Skript.instance(), new NameableNameHandler());
    }

    private static class NameableNameHandler
    implements ExpressionPropertyHandler<Nameable, Component> {
        private NameableNameHandler() {
        }

        @Override
        public Component convert(Nameable nameable) {
            return nameable.customName();
        }

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
                return new Class[]{Component.class};
            }
            return null;
        }

        @Override
        public void change(Nameable nameable, Object @Nullable [] delta, Changer.ChangeMode mode) {
            assert (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET);
            Component name = delta == null ? null : (Component)delta[0];
            nameable.customName(name);
        }

        @Override
        @NotNull
        public Class<Component> returnType() {
            return Component.class;
        }
    }
}

