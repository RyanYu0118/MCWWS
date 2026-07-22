/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.regions.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.ScriptWarning;

@Name(value="Is Member/Owner of Region")
@Description(value={"Checks whether a player is a member or owner of a particular region.", "This condition requires a supported regions plugin to be installed."})
@Example(value="on region enter:\n\tplayer is the owner of the region\n\tmessage \"Welcome back to %region%!\"\n\tsend \"%player% just entered %region%!\" to all members of the region\n")
@Since(value={"2.1"})
@RequiredPlugins(value={"Supported regions plugin"})
public class CondIsMember
extends Condition {
    private Expression<OfflinePlayer> players;
    Expression<Region> regions;
    boolean owner;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.regions = exprs[1];
        this.owner = parseResult.mark == 1;
        this.setNegated(matchedPattern == 1);
        ScriptWarning.printDeprecationWarning("Skript's region syntaxes are deprecated and will be removed in a future release. For WorldGuard support, we recommend using skript-worldguard: https://github.com/SkriptLang/skript-worldguard");
        return true;
    }

    @Override
    public boolean check(Event event) {
        return this.players.check(event, player -> this.regions.check(event, region -> this.owner ? region.isOwner((OfflinePlayer)player) : region.isMember((OfflinePlayer)player), this.isNegated()));
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.players.toString(e, debug) + " " + (this.players.isSingle() ? "is" : "are") + (this.isNegated() ? " not" : "") + " " + (this.owner ? "owner" : "member") + (this.players.isSingle() ? "" : "s") + " of " + this.regions.toString(e, debug);
    }

    static {
        Skript.registerCondition(CondIsMember.class, "%offlineplayers% (is|are) (0\u00a6[a] member|1\u00a6[(the|an)] owner) of [[the] region] %regions%", "%offlineplayers% (is|are)(n't| not) (0\u00a6[a] member|1\u00a6[(the|an)] owner) of [[the] region] %regions%");
    }
}

