/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.item.elements;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.item.elements.ExprLore;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Item with Lore")
@Description(value={"Returns a copy of an item with with new lore.", "If passing multiple components, each with be a line of lore."})
@Example(value="set {_item} to stone with lore \"line 1\" and \"line 2\"\ngive {_item} to player\n")
@Since(value={"2.3"})
public class ExprItemWithLore
extends PropertyExpression<ItemType, ItemType> {
    private Expression<?> lore;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprItemWithLore.class, ItemType.class).supplier(ExprItemWithLore::new)).priority(DEFAULT_PRIORITY)).addPattern("%itemtype% with [a|the] lore %textcomponents/strings%")).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.lore = ExprLore.convertStrings(exprs[1]);
        return true;
    }

    protected ItemType[] get(Event event, ItemType[] source) {
        List<Component> lore = ExprLore.parseLore(this.lore.getArray(event));
        return this.get(source, item -> {
            item = item.clone();
            ItemMeta meta = item.getItemMeta();
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        });
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.getExpr().toString(event, debug) + " with lore " + this.lore.toString(event, debug);
    }
}

