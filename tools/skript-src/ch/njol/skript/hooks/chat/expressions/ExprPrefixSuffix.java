/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.chat.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.hooks.VaultHook;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Prefix/Suffix")
@Description(value={"The prefix or suffix as defined in the server's chat plugin."})
@Example.Examples(value={@Example(value="on chat:\n\tcancel event\n\tbroadcast \"%player's prefix%%player's display name%%player's suffix%: %message%\" to the player's world\n"), @Example(value="set the player's prefix to \"[<red>Admin<reset>] \""), @Example(value="clear player's prefix")})
@Since(value={"2.0, 2.10 (delete)"})
@RequiredPlugins(value={"Vault", "a chat plugin that supports Vault"})
public class ExprPrefixSuffix
extends SimplePropertyExpression<Player, String> {
    private boolean prefix;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.prefix = parseResult.mark == 1;
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public String convert(Player player) {
        return this.prefix ? VaultHook.chat.getPlayerPrefix(player) : VaultHook.chat.getPlayerSuffix(player);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Class[] classArray;
        switch (mode) {
            case SET: {
                Class[] classArray2 = new Class[1];
                classArray = classArray2;
                classArray2[0] = String.class;
                break;
            }
            case RESET: 
            case DELETE: {
                classArray = new Class[]{};
                break;
            }
            default: {
                classArray = null;
            }
        }
        return classArray;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        CompletableFuture.runAsync(() -> {
            block4: for (Player player : (Player[])this.getExpr().getArray(event)) {
                switch (mode) {
                    case SET: {
                        if (this.prefix) {
                            VaultHook.chat.setPlayerPrefix(player, (String)delta[0]);
                            continue block4;
                        }
                        VaultHook.chat.setPlayerSuffix(player, (String)delta[0]);
                        continue block4;
                    }
                    case RESET: 
                    case DELETE: {
                        if (this.prefix) {
                            VaultHook.chat.setPlayerPrefix(player, null);
                            continue block4;
                        }
                        VaultHook.chat.setPlayerSuffix(player, null);
                    }
                }
            }
        }).join();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return this.prefix ? "prefix" : "suffix";
    }

    static {
        ExprPrefixSuffix.register(ExprPrefixSuffix.class, String.class, "[chat] (1:prefix|2:suffix)", "players");
    }
}

