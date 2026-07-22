/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;

@Name(value="Is Custom Name Visible")
@Description(value={"Checks if an entity's custom name is visible."})
@Example(value="send true if target's custom name is visible")
@Since(value={"2.10"})
public class CondIsCustomNameVisible
extends PropertyCondition<Entity> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(matchedPattern == 1 || matchedPattern == 3);
        this.setExpr(exprs[0]);
        return true;
    }

    @Override
    public boolean check(Entity entity) {
        return entity.isCustomNameVisible();
    }

    @Override
    protected String getPropertyName() {
        return "custom name";
    }

    static {
        Skript.registerCondition(CondIsCustomNameVisible.class, "%entities%'[s] custom name[s] (is|are) visible", "%entities%'[s] custom name[s] (isn't|is not|are not|aren't) visible", "custom name of %entities% (is|are) visible", "custom name of %entities% (isn't|is not|are not|aren't) visible");
    }
}

