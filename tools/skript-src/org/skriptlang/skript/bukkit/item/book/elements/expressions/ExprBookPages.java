/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Material
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
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Book Pages")
@Description(value={"The pages of a book (Supports Skript's chat format)", "Note: In order to modify the pages of a new written book, you must have the title and author", "of the book set. Skript will do this for you, but if you want your own, please set those values."})
@Example.Examples(value={@Example(value="on book sign:\n\tif the number of pages of event-item is greater than 1:\n\t\tmessage \"The second page of the authored book is: %page 2 of event-item%\"\n"), @Example(value="set page 1 of the player's held item to \"This page was written with Skript!\"")})
@Since(value={"2.2-dev31, 2.7 (changers)"})
public class ExprBookPages
extends SimpleExpression<Component> {
    private Expression<ItemType> books;
    @Nullable
    private Expression<Integer> pageNumber;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprBookPages.class, Component.class).supplier(ExprBookPages::new)).priority(PropertyExpression.DEFAULT_PRIORITY)).addPatterns("[all [[of] the]|the] [book] (pages|content) of %itemtypes%", "%itemtypes%'[s] [book] (pages|content)", "[book] page %integer% of %itemtypes%", "%itemtypes%'[s] [book] page %integer%")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0 || matchedPattern == 1) {
            this.books = expressions[0];
        } else if (matchedPattern == 2) {
            this.pageNumber = expressions[0];
            this.books = expressions[1];
        } else {
            this.books = expressions[0];
            this.pageNumber = expressions[1];
        }
        return true;
    }

    protected Component[] get(Event event) {
        ArrayList<Component> pages = new ArrayList<Component>();
        for (ItemType book : this.books.getArray(event)) {
            ItemMeta itemMeta;
            if (book.getMaterial() != Material.WRITTEN_BOOK || !((itemMeta = book.getItemMeta()) instanceof BookMeta)) {
                return new Component[0];
            }
            BookMeta bookMeta = (BookMeta)itemMeta;
            if (this.isAllPages()) {
                pages.addAll(bookMeta.pages());
                continue;
            }
            Integer pageNumber = this.pageNumber.getSingle(event);
            if (pageNumber == null || pageNumber <= 0 || pageNumber > bookMeta.getPageCount()) continue;
            pages.add(bookMeta.page(pageNumber.intValue()));
        }
        return pages.toArray(new Component[0]);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET -> CollectionUtils.array(this.isAllPages() ? Component[].class : Component.class);
            case Changer.ChangeMode.ADD -> {
                if (this.isAllPages()) {
                    yield CollectionUtils.array(Component[].class);
                }
                yield null;
            }
            case Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        ArrayList<Component> newPages;
        int pageNumber = this.isAllPages() ? -1 : this.pageNumber.getOptionalSingle(event).orElse(-1);
        ArrayList<Component> arrayList = newPages = delta == null ? Collections.emptyList() : new ArrayList<Component>(delta.length);
        if (delta != null) {
            for (Object page : delta) {
                newPages.add((Component)page);
            }
            if (pageNumber != -1 && newPages.isEmpty()) {
                return;
            }
        }
        for (ItemType book : this.books.getArray(event)) {
            BookMeta bookMeta;
            block16: {
                block15: {
                    ItemMeta itemMeta;
                    if (book.getMaterial() != Material.WRITTEN_BOOK || !((itemMeta = book.getItemMeta()) instanceof BookMeta)) continue;
                    bookMeta = (BookMeta)itemMeta;
                    if (!this.isAllPages()) break block15;
                    switch (mode) {
                        case SET: 
                        case DELETE: 
                        case RESET: {
                            bookMeta.pages(newPages);
                            break block16;
                        }
                        case ADD: {
                            bookMeta.addPages(newPages.toArray(new Component[0]));
                            break block16;
                        }
                        default: {
                            throw new IllegalStateException();
                        }
                    }
                }
                switch (mode) {
                    case SET: {
                        bookMeta.page(pageNumber, (Component)newPages.getFirst());
                        break;
                    }
                    case DELETE: {
                        ArrayList pages = new ArrayList(bookMeta.pages());
                        pages.remove(pageNumber);
                        bookMeta.pages(pages);
                        break;
                    }
                    case RESET: {
                        bookMeta.page(pageNumber, (Component)Component.empty());
                        break;
                    }
                    default: {
                        throw new IllegalStateException();
                    }
                }
            }
            if (!bookMeta.hasTitle()) {
                Component title = bookMeta.hasDisplayName() ? bookMeta.displayName() : Component.text((String)"Written Book");
                bookMeta.title(title);
            }
            if (!bookMeta.hasAuthor()) {
                bookMeta.author((Component)Component.text((String)"Unknown"));
            }
            book.setItemMeta((ItemMeta)bookMeta);
        }
    }

    private boolean isAllPages() {
        return this.pageNumber == null;
    }

    @Override
    public boolean isSingle() {
        return this.books.isSingle() && !this.isAllPages();
    }

    @Override
    public Class<? extends Component> getReturnType() {
        return Component.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.isAllPages()) {
            builder.append((Object)"all of the pages");
        } else {
            builder.append("page", this.pageNumber);
        }
        builder.append("of", this.books);
        return builder.toString();
    }
}

