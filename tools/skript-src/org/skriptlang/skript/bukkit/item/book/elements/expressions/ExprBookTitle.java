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

@Name(value="Book Title")
@Description(value={"The title of a book."})
@Example(value="on book sign:\n\tmessage \"You finished your book titled %title of event-item%\"\n")
@Since(value={"2.2-dev31"})
public class ExprBookTitle
extends SimplePropertyExpression<ItemType, Component> {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprBookTitle.infoBuilder(ExprBookTitle.class, Component.class, "book (name|title)", "itemtypes", false).supplier(ExprBookTitle::new)).build());
    }

    @Override
    public Component convert(ItemType item) {
        BookMeta bookMeta;
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof BookMeta && (bookMeta = (BookMeta)itemMeta).hasTitle()) {
            return bookMeta.title();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(Component.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Component title = delta == null ? null : (Component)delta[0];
        for (ItemType item : (ItemType[])this.getExpr().getArray(event)) {
            ItemMeta itemMeta = item.getItemMeta();
            if (!(itemMeta instanceof BookMeta)) continue;
            BookMeta bookMeta = (BookMeta)itemMeta;
            bookMeta.title(title);
            item.setItemMeta((ItemMeta)bookMeta);
        }
    }

    @Override
    public Class<? extends Component> getReturnType() {
        return Component.class;
    }

    @Override
    protected String getPropertyName() {
        return "book title";
    }
}

