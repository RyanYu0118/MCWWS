/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.types;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.types.EntityClassInfo;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

@ApiStatus.Internal
public class PlayerClassInfo
extends ClassInfo<Player> {
    public PlayerClassInfo() {
        super(Player.class, "player");
        this.user("players?").name("Player").description("A player. Depending on whether a player is online or offline several actions can be performed with them, though you won't get any errors when using effects that only work if the player is online (e.g. changing their inventory) on an offline player.", "You have two possibilities to use players as command arguments: <player> and <offline player>. The first requires that the player is online and also accepts only part of the name, while the latter doesn't require that the player is online, but the player's name has to be entered exactly.").usage("Parsing an offline player as a player (online) will return nothing (none), for that case you would need to parse as offlineplayer which only returns nothing (none) if player doesn't exist in Minecraft databases (name not taken) otherwise it will return the player regardless of their online status.").examples("set {_p} to \"Notch\" parsed as a player # returns <none> unless Notch is actually online or starts with Notch like Notchan", "set {_p} to \"N\" parsed as a player # returns Notch if Notch is online because their name starts with 'N' (case insensitive) however, it would return nothing if no player whose name starts with 'N' is online.").since("1.0").defaultExpression(new EventValueExpression<Player>(Player.class)).after("string", "world").parser(new PlayerParser()).changer(new PlayerChanger()).property(Property.NAME, "A player's account/true name, as text. Cannot be changed.", Skript.instance(), ExpressionPropertyHandler.of(CommandSender::name, Component.class)).property(Property.DISPLAY_NAME, "The player's display name, as text. Can be set or reset.", Skript.instance(), new PlayerDisplayNameHandler()).serializeAs(OfflinePlayer.class);
    }

    public static class PlayerParser
    extends Parser<Player> {
        @Override
        @Nullable
        public Player parse(String string, ParseContext context) {
            if (context == ParseContext.COMMAND || context == ParseContext.PARSE) {
                if (string.isEmpty()) {
                    return null;
                }
                if (Utils.isValidUUID(string)) {
                    return Bukkit.getPlayer((UUID)UUID.fromString(string));
                }
                String name = string.toLowerCase(Locale.ENGLISH);
                int nameLength = name.length();
                ArrayList<Player> players = new ArrayList<Player>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getName().toLowerCase(Locale.ENGLISH).startsWith(name)) continue;
                    if (player.getName().length() == nameLength) {
                        return player;
                    }
                    players.add(player);
                }
                if (players.size() == 1) {
                    return (Player)players.get(0);
                }
                if (players.isEmpty()) {
                    Skript.error(String.format(Language.get("commands.no player starts with"), string));
                } else {
                    Skript.error(String.format(Language.get("commands.multiple players start with"), string));
                }
                return null;
            }
            assert (false);
            return null;
        }

        @Override
        public boolean canParse(ParseContext context) {
            return context == ParseContext.COMMAND || context == ParseContext.PARSE;
        }

        @Override
        public String toString(Player player, int flags) {
            return player.getName();
        }

        @Override
        public String toVariableNameString(Player player) {
            if (SkriptConfig.usePlayerUUIDsInVariableNames.value().booleanValue()) {
                return player.getUniqueId().toString();
            }
            return player.getName();
        }

        @Override
        public String getDebugMessage(Player player) {
            return player.getName() + " " + Classes.getDebugMessage(player.getLocation());
        }
    }

    public static class PlayerChanger
    implements Changer<Player> {
        private static final Changer<Entity> ENTITY_CHANGER = new EntityClassInfo.EntityChanger();

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.DELETE) {
                return null;
            }
            return ENTITY_CHANGER.acceptChange(mode);
        }

        public void change(Player[] players, Object @Nullable [] delta, Changer.ChangeMode mode) {
            ENTITY_CHANGER.change((Entity[])players, delta, mode);
        }
    }

    public static class PlayerDisplayNameHandler
    implements ExpressionPropertyHandler<Player, Component> {
        @Override
        public Component convert(Player player) {
            return player.displayName();
        }

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            return switch (mode) {
                case Changer.ChangeMode.SET, Changer.ChangeMode.RESET -> CollectionUtils.array(Component.class);
                default -> null;
            };
        }

        @Override
        public void change(Player player, Object @Nullable [] delta, Changer.ChangeMode mode) {
            Component name = delta == null ? null : (Component)delta[0];
            player.displayName(name);
        }

        @Override
        @NotNull
        public Class<Component> returnType() {
            return Component.class;
        }
    }
}

