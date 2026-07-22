/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Has Permission")
@Description(value={"Test whether a player has a certain permission."})
@Example.Examples(value={@Example(value="player has permission \"skript.tree\""), @Example(value="victim has the permission \"admin\":\n\tsend \"You're attacking an admin!\" to attacker\n")})
@Since(value={"1.0"})
public class CondPermission
extends Condition {
    private Expression<String> permissions;
    private Expression<CommandSender> senders;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.senders = exprs[0];
        this.permissions = exprs[1];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        return this.senders.check(e, s -> this.permissions.check(e, perm -> {
            if (s.hasPermission(perm)) {
                return true;
            }
            if (perm.startsWith("skript.")) {
                int i = perm.lastIndexOf(46);
                while (i != -1) {
                    if (s.hasPermission(perm.substring(0, i + 1) + "*")) {
                        return true;
                    }
                    i = perm.lastIndexOf(46, i - 1);
                }
            }
            return false;
        }), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.HAVE, e, debug, this.senders, "the permission" + (this.permissions.isSingle() ? " " : "s ") + this.permissions.toString());
    }

    static {
        Skript.registerCondition(CondPermission.class, "%commandsenders% (has|have) [the] permission[s] %strings%", "%commandsenders% (doesn't|does not|do not|don't) have [the] permission[s] %strings%");
    }
}

