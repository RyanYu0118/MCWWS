/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.conditions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;

@Name(value="Is Unbreakable")
@Description(value={"Checks whether an item is unbreakable."})
@Example.Examples(value={@Example(value="if event-item is unbreakable:\n\tsend \"This item is unbreakable!\" to player\n"), @Example(value="if tool of {_p} is breakable:\n\tsend \"Your tool is breakable!\" to {_p}\n")})
@Since(value={"2.5.1, 2.9.0 (breakable)"})
public class CondIsUnbreakable
extends PropertyCondition<ItemType> {
    private boolean breakable;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.breakable = !parseResult.hasTag("un");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(ItemType item) {
        return item.getItemMeta().isUnbreakable() ^ this.breakable;
    }

    @Override
    protected String getPropertyName() {
        return this.breakable ? "breakable" : "unbreakable";
    }

    static {
        CondIsUnbreakable.register(CondIsUnbreakable.class, "[:un]breakable", "itemtypes");
    }
}

