/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.EffEnforceWhitelist;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Whitelist")
@Description(value={"An expression for obtaining and modifying the server's whitelist.", "Players may be added and removed from the whitelist.", "The whitelist can be enabled or disabled by setting the whitelist to true or false respectively."})
@Example.Examples(value={@Example(value="set the whitelist to false"), @Example(value="add all players to whitelist"), @Example(value="reset the whitelist")})
@Since(value={"2.5.2, 2.9.0 (delete)"})
public class ExprWhitelist
extends SimpleExpression<OfflinePlayer> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    protected OfflinePlayer[] get(Event event) {
        return Bukkit.getServer().getWhitelistedPlayers().toArray(new OfflinePlayer[0]);
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case ADD: 
            case REMOVE: {
                return CollectionUtils.array(OfflinePlayer.class);
            }
            case DELETE: 
            case RESET: 
            case SET: {
                return CollectionUtils.array(Boolean.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        switch (mode) {
            case SET: {
                boolean toggle = (Boolean)delta[0];
                Bukkit.setWhitelist((boolean)toggle);
                if (!toggle) break;
                EffEnforceWhitelist.reloadWhitelist();
                break;
            }
            case ADD: {
                for (Object player : delta) {
                    ((OfflinePlayer)player).setWhitelisted(true);
                }
                break;
            }
            case REMOVE: {
                for (Object player : delta) {
                    ((OfflinePlayer)player).setWhitelisted(false);
                }
                EffEnforceWhitelist.reloadWhitelist();
                break;
            }
            case DELETE: 
            case RESET: {
                for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
                    player.setWhitelisted(false);
                }
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends OfflinePlayer> getReturnType() {
        return OfflinePlayer.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "whitelist";
    }

    static {
        Skript.registerExpression(ExprWhitelist.class, OfflinePlayer.class, ExpressionType.SIMPLE, "[the] white[ ]list");
    }
}

