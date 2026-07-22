/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.meta.ItemMeta
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.inventory.meta.ItemMeta;

@Name(value="Unbreakable Items")
@Description(value={"Creates breakable or unbreakable copies of given items."})
@Example.Examples(value={@Example(value="set {_item} to unbreakable iron sword"), @Example(value="give breakable {_weapon} to all players")})
@Since(value={"2.2-dev13b, 2.9.0 (breakable)"})
public class ExprUnbreakable
extends SimplePropertyExpression<ItemType, ItemType> {
    private boolean unbreakable;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.unbreakable = parseResult.hasTag("un");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public ItemType convert(ItemType itemType) {
        ItemType clone = itemType.clone();
        ItemMeta meta = clone.getItemMeta();
        meta.setUnbreakable(this.unbreakable);
        clone.setItemMeta(meta);
        return clone;
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    protected String getPropertyName() {
        return this.unbreakable ? "unbreakable" : "breakable";
    }

    static {
        Skript.registerExpression(ExprUnbreakable.class, ItemType.class, ExpressionType.PROPERTY, "[:un]breakable %itemtypes%");
    }
}

