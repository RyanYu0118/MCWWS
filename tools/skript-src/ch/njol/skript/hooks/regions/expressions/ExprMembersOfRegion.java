/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.regions.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.hooks.regions.RegionsPlugin;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.ScriptWarning;

@Name(value="Region Members & Owners")
@Description(value={"A list of members or owners of a <a href='#region'>region</a>.", "This expression requires a supported regions plugin to be installed."})
@Example(value="on entering of a region:\n\tmessage \"You're entering %region% whose owners are %owners of region%\"\n")
@Since(value={"2.1"})
@RequiredPlugins(value={"Supported regions plugin"})
public class ExprMembersOfRegion
extends SimpleExpression<OfflinePlayer> {
    private boolean owners;
    private Expression<Region> regions;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.regions = exprs[0];
        this.owners = parseResult.mark == 1;
        ScriptWarning.printDeprecationWarning("Skript's region syntaxes are deprecated and will be removed in a future release. For WorldGuard support, we recommend using skript-worldguard: https://github.com/SkriptLang/skript-worldguard");
        return true;
    }

    protected OfflinePlayer[] get(Event e) {
        ArrayList<OfflinePlayer> r = new ArrayList<OfflinePlayer>();
        for (Region region : this.regions.getArray(e)) {
            r.addAll(this.owners ? region.getOwners() : region.getMembers());
        }
        return r.toArray(new OfflinePlayer[r.size()]);
    }

    @Override
    public boolean isSingle() {
        return this.owners && this.regions.isSingle() && !RegionsPlugin.hasMultipleOwners();
    }

    @Override
    public Class<? extends OfflinePlayer> getReturnType() {
        return OfflinePlayer.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the " + (String)(this.owners ? "owner" + (this.isSingle() ? "" : "s") : "members") + " of " + this.regions.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprMembersOfRegion.class, OfflinePlayer.class, ExpressionType.PROPERTY, "(all|the|) (0\u00a6members|1\u00a6owner[s]) of [[the] region[s]] %regions%", "[[the] region[s]] %regions%'[s] (0\u00a6members|1\u00a6owner[s])");
    }
}

