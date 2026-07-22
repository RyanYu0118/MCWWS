/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Has Scoreboard Tag")
@Description(value={"Checks whether the given entities has the given <a href='#ExprScoreboardTags'>scoreboard tags</a>."})
@Example(value="if the targeted armor stand has the scoreboard tag \"test tag\":")
@Since(value={"2.3"})
public class CondHasScoreboardTag
extends Condition {
    private Expression<Entity> entities;
    private Expression<String> tags;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.tags = exprs[1];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        String[] tagsList = this.tags.getAll(event);
        return this.entities.check(event, entity -> SimpleExpression.check(tagsList, tag -> entity.getScoreboardTags().contains(tag), false, this.tags.getAnd()), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.HAVE, event, debug, this.entities, "the scoreboard " + (this.tags.isSingle() ? "tag " : "tags ") + this.tags.toString(event, debug));
    }

    static {
        PropertyCondition.register(CondHasScoreboardTag.class, PropertyCondition.PropertyType.HAVE, "[the] score[ ]board tag[s] %strings%", "entities");
    }
}

