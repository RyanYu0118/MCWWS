/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
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
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.ScriptWarning;

@Name(value="Region Contains")
@Description(value={"Checks whether a location is contained in a particular <a href='#region'>region</a>.", "This condition requires a supported regions plugin to be installed."})
@Example.Examples(value={@Example(value="player is in the region {regions::3}"), @Example(value="on region enter:\n\tregion contains {flags.%world%.red}\n\tmessage \"The red flag is near!\"\n")})
@Since(value={"2.1"})
@RequiredPlugins(value={"Supported regions plugin"})
public class CondRegionContains
extends Condition {
    private Expression<Region> regions;
    Expression<Location> locs;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (exprs.length == 3) {
            this.regions = exprs[0];
            this.locs = Direction.combine(exprs[1], exprs[2]);
        } else {
            this.regions = exprs[1];
            this.locs = exprs[0];
        }
        this.setNegated(matchedPattern >= 2);
        ScriptWarning.printDeprecationWarning("Skript's region syntaxes are deprecated and will be removed in a future release. For WorldGuard support, we recommend using skript-worldguard: https://github.com/SkriptLang/skript-worldguard");
        return true;
    }

    @Override
    public boolean check(Event event) {
        return this.regions.check(event, region -> this.locs.check(event, region::contains, this.isNegated()));
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.regions.toString(e, debug) + " contain" + (this.regions.isSingle() ? "s" : "") + " " + this.locs.toString(e, debug);
    }

    static {
        Skript.registerCondition(CondRegionContains.class, "[[the] region] %regions% contain[s] %directions% %locations%", "%locations% (is|are) ([contained] in|part of) [[the] region] %regions%", "[[the] region] %regions% (do|does)(n't| not) contain %directions% %locations%", "%locations% (is|are)(n't| not) (contained in|part of) [[the] region] %regions%");
    }
}

