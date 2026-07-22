/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.meta.BookMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.item.book.elements.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Book Author")
@Description(value={"The author of a book."})
@Example(value="on book sign:\n\tbroadcast \"A new book has been created by %author of event-item%\"\n")
@Since(value={"2.2-dev31"})
public class ExprBookAuthor
extends SimplePropertyExpression<ItemType, Component> {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprBookAuthor.infoBuilder(ExprBookAuthor.class, Component.class, "[book] (author|writer|publisher)", "itemtypes", false).supplier(ExprBookAuthor::new)).build());
    }

    @Override
    @Nullable
    public Component convert(ItemType item) {
        BookMeta bookMeta;
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof BookMeta && (bookMeta = (BookMeta)itemMeta).hasAuthor()) {
            return bookMeta.author();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE -> CollectionUtils.array(Component.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Component author = delta == null ? null : (Component)delta[0];
        for (ItemType item : (ItemType[])this.getExpr().getArray(event)) {
            ItemMeta itemMeta = item.getItemMeta();
            if (!(itemMeta instanceof BookMeta)) continue;
            BookMeta bookMeta = (BookMeta)itemMeta;
            bookMeta.author(author);
            item.setItemMeta((ItemMeta)bookMeta);
        }
    }

    @Override
    public Class<? extends Component> getReturnType() {
        return Component.class;
    }

    @Override
    protected String getPropertyName() {
        return "book author";
    }
}

