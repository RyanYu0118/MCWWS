/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.Tag
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.tags.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.Comparator;
import java.util.TreeSet;
import org.bukkit.Keyed;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagModule;
import org.skriptlang.skript.bukkit.tags.TagType;
import org.skriptlang.skript.bukkit.tags.sources.TagOrigin;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="All Tags of a Type")
@Description(value={"Returns all the tags.", "`minecraft tag` will return only the vanilla tags, `datapack tag` will return only datapack-provided tags, `paper tag` will return only Paper's custom tags (if you are running Paper), and `custom tag` will look in the \"skript\" namespace for custom tags you've registered.", "You can also filter by tag types using \"item\", \"block\", or \"entity\"."})
@Example.Examples(value={@Example(value="broadcast minecraft tags"), @Example(value="send paper entity tags"), @Example(value="broadcast all block tags")})
@Since(value={"2.10"})
@Keywords(value={"blocks", "minecraft tag", "type", "category"})
public class ExprTagsOfType
extends SimpleExpression<Tag> {
    TagType<?>[] types;
    private TagOrigin origin;
    private boolean datapackOnly;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprTagsOfType.class, Tag.class).addPattern("[all [[of] the]|the] " + TagOrigin.getFullPattern() + " " + TagType.getFullPattern() + " tags")).supplier(ExprTagsOfType::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.types = TagType.fromParseMark(parseResult.mark);
        this.origin = TagOrigin.fromParseTags(parseResult.tags);
        this.datapackOnly = this.origin == TagOrigin.BUKKIT && parseResult.hasTag("datapack");
        return true;
    }

    protected Tag<?> @Nullable [] get(Event event) {
        TreeSet<Tag> tags = new TreeSet<Tag>(Comparator.comparing(Keyed::key));
        for (TagType<?> type : this.types) {
            for (Tag<?> tag2 : TagModule.tagRegistry.getMatchingTags(this.origin, type, tag -> this.origin != TagOrigin.BUKKIT || this.datapackOnly ^ tag.getKey().getNamespace().equals("minecraft"))) {
                tags.add(tag2);
            }
        }
        return tags.toArray(new Tag[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<Tag> getReturnType() {
        return Tag.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String registry = this.types.length > 1 ? "" : " " + this.types[0].toString();
        return "all of the " + this.origin.toString(this.datapackOnly) + registry + " tags";
    }
}

