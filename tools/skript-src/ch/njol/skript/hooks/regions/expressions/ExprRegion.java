/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.hooks.regions.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.lang.script.ScriptWarning;

@Name(value="Region")
@Description(value={"The <a href='#region'>region</a> involved in an event.", "This expression requires a supported regions plugin to be installed."})
@Example(value="on region enter:\n\tregion is {forbidden region}\n\tcancel the event\n")
@Since(value={"2.1"})
@RequiredPlugins(value={"Supported regions plugin"})
public class ExprRegion
extends EventValueExpression<Region> {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        ScriptWarning.printDeprecationWarning("Skript's region syntaxes are deprecated and will be removed in a future release. For WorldGuard support, we recommend using skript-worldguard: https://github.com/SkriptLang/skript-worldguard");
        return super.init(expressions, matchedPattern, isDelayed, parser);
    }

    public ExprRegion() {
        super(Region.class);
    }

    static {
        ExprRegion.register(ExprRegion.class, Region.class, "[event-]region");
    }
}

