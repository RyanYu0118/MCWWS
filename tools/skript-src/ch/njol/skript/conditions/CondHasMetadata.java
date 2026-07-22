/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.metadata.Metadatable
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
import org.bukkit.event.Event;
import org.bukkit.metadata.Metadatable;
import org.jetbrains.annotations.Nullable;

@Name(value="Has Metadata")
@Description(value={"Checks whether a metadata holder has a metadata tag."})
@Example(value="if player has metadata value \"healer\":")
@Since(value={"2.2-dev36"})
public class CondHasMetadata
extends Condition {
    private Expression<Metadatable> holders;
    private Expression<String> values;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.holders = exprs[0];
        this.values = exprs[1];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        return this.holders.check(e, holder -> this.values.check(e, arg_0 -> ((Metadatable)holder).hasMetadata(arg_0)), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.HAVE, e, debug, this.holders, "metadata " + (this.values.isSingle() ? "value " : "values ") + this.values.toString(e, debug));
    }

    static {
        Skript.registerCondition(CondHasMetadata.class, "%metadataholders% (has|have) metadata [(value|tag)[s]] %strings%", "%metadataholders% (doesn't|does not|do not|don't) have metadata [(value|tag)[s]] %strings%");
    }
}

