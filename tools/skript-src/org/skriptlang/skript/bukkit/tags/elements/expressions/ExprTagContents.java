/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.Tag
 *  org.bukkit.entity.EntityType
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.tags.elements.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagType;
import org.skriptlang.skript.bukkit.tags.elements.expressions.ExprTag;
import org.skriptlang.skript.bukkit.tags.elements.expressions.ExprTagsOf;
import org.skriptlang.skript.bukkit.tags.elements.expressions.ExprTagsOfType;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Tags Contents")
@Description(value={"Returns all the values that a tag contains.", "For item and block tags, this will return items. For entity tags, it will return entity datas (a creeper, a zombie)."})
@Example.Examples(value={@Example(value="broadcast tag values of minecraft tag \"dirt\""), @Example(value="broadcast (first element of player's tool's block tags)'s tag contents")})
@Since(value={"2.10"})
@Keywords(value={"blocks", "minecraft tag", "type", "category"})
public class ExprTagContents
extends SimpleExpression<Object> {
    private Expression<Tag<?>> tag;
    private Class<?> returnType;
    private Class<?>[] possibleReturnTypes;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)PropertyExpression.infoBuilder(ExprTagContents.class, Object.class, "tag (contents|values)", "minecrafttag", false).supplier(ExprTagContents::new)).build());
    }

    @Override
    public boolean init(Expression<?> @NotNull [] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.tag = expressions[0];
        TagType<?>[] tagTypes = null;
        Expression<?> expression = expressions[0];
        if (expression instanceof ExprTag) {
            ExprTag exprTag = (ExprTag)expression;
            tagTypes = exprTag.types;
        } else {
            expression = expressions[0];
            if (expression instanceof ExprTagsOf) {
                ExprTagsOf exprTagsOf = (ExprTagsOf)expression;
                tagTypes = exprTagsOf.types;
            } else {
                expression = expressions[0];
                if (expression instanceof ExprTagsOfType) {
                    ExprTagsOfType exprTagsOfType = (ExprTagsOfType)expression;
                    tagTypes = exprTagsOfType.types;
                }
            }
        }
        if (tagTypes != null) {
            this.possibleReturnTypes = new Class[tagTypes.length];
            for (int i = 0; i < tagTypes.length; ++i) {
                Class<Object> type = tagTypes[i].type();
                if (type == Material.class) {
                    type = ItemType.class;
                } else if (type == EntityType.class) {
                    type = EntityData.class;
                }
                this.possibleReturnTypes[i] = type;
            }
            this.returnType = Classes.getSuperClassInfo(this.possibleReturnTypes).getC();
        } else {
            this.returnType = Object.class;
            this.possibleReturnTypes = new Class[]{this.returnType};
        }
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        return this.stream(event).toArray(length -> Array.newInstance(this.getReturnType(), length));
    }

    @Override
    public Stream<?> stream(Event event) {
        Tag<?> tag = this.tag.getSingle(event);
        if (tag == null) {
            return Stream.empty();
        }
        return tag.getValues().stream().map(value -> {
            if (value instanceof Material) {
                Material material = (Material)value;
                return new ItemType(material);
            }
            if (value instanceof EntityType) {
                EntityType entityType = (EntityType)value;
                return EntityUtils.toSkriptEntityData(entityType);
            }
            return null;
        }).filter(Objects::nonNull);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<?> getReturnType() {
        return this.returnType;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return this.possibleReturnTypes;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the tag contents of " + this.tag.toString(event, debug);
    }
}

