/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.milkbowl.vault.permission.Permission
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.permission.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.hooks.VaultHook;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Group")
@Description(value={"The primary group or all groups of a player. This expression requires Vault and a compatible permissions plugin to be installed.", "If you have LuckPerms, ensure you have vault integration enabled in the luck perms configurations."})
@Example(value="on join:\n\tbroadcast \"%group of player%\" # this is the player's primary group\n\tbroadcast \"%groups of player%\" # this is all of the player's groups\n")
@Since(value={"2.2-dev35"})
@RequiredPlugins(value={"Vault", "a permission plugin that supports Vault"})
public class ExprGroup
extends SimpleExpression<String> {
    private boolean primary;
    @Nullable
    private Expression<OfflinePlayer> players;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!VaultHook.permission.hasGroupSupport()) {
            Skript.error("The permissions plugin you are using does not support groups.");
            return false;
        }
        this.players = exprs[0];
        this.primary = !parseResult.hasTag("plural");
        return true;
    }

    protected String[] get(Event event) {
        OfflinePlayer[] players = this.players.getArray(event);
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<String> groups = new ArrayList<String>();
            for (OfflinePlayer player : players) {
                if (this.primary) {
                    groups.add(VaultHook.permission.getPrimaryGroup(null, player));
                    continue;
                }
                Collections.addAll(groups, VaultHook.permission.getPlayerGroups(null, player));
            }
            return groups.toArray(new String[0]);
        }).join();
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE || mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
            return new Class[]{String[].class};
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Permission api = VaultHook.permission;
        block5: for (OfflinePlayer player : this.players.getArray(e)) {
            switch (mode) {
                case ADD: {
                    for (Object o : delta) {
                        api.playerAddGroup(null, player, (String)o);
                    }
                    continue block5;
                }
                case REMOVE: {
                    for (Object o : delta) {
                        api.playerRemoveGroup(null, player, (String)o);
                    }
                    continue block5;
                }
                case RESET: 
                case DELETE: 
                case SET: {
                    for (String group : api.getPlayerGroups(null, player)) {
                        api.playerRemoveGroup(null, player, group);
                    }
                    if (mode != Changer.ChangeMode.SET) continue block5;
                    for (Object o : delta) {
                        api.playerAddGroup(null, player, (String)o);
                    }
                    continue block5;
                }
            }
        }
    }

    @Override
    public boolean isSingle() {
        return this.players.isSingle() && this.primary;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "group" + (this.primary ? "" : "s") + " of " + this.players.toString(event, debug);
    }

    static {
        PropertyExpression.register(ExprGroup.class, String.class, "group[plural:s]", "offlineplayers");
    }
}

