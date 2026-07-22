/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.Tag
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.tags.elements.conditions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Keyed;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagModule;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Is Tagged")
@Description(value={"Checks whether an item, block, entity, or entitydata is tagged with the given tag."})
@Example.Examples(value={@Example(value="if player's tool is tagged with minecraft tag \"enchantable/sharp_weapon\":\n\tenchant player's tool with sharpness 1\n"), @Example(value="if all logs are tagged with tag \"minecraft:logs\"")})
@Since(value={"2.10"})
@Keywords(value={"blocks", "minecraft tag", "type", "category"})
public class CondIsTagged
extends Condition {
    private Expression<Tag<Keyed>> tags;
    private Expression<?> elements;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, PropertyCondition.infoBuilder(CondIsTagged.class, PropertyCondition.PropertyType.BE, "tagged (as|with) %minecrafttags%", "itemtypes/entities/entitydatas").supplier(CondIsTagged::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.elements = expressions[0];
        this.tags = expressions[1];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        Tag<Keyed>[] tags = this.tags.getAll(event);
        if (tags.length == 0) {
            return this.isNegated();
        }
        boolean and = this.tags.getAnd();
        return this.elements.check(event, element -> {
            ItemType itemType;
            boolean isAny = element instanceof ItemType && !(itemType = (ItemType)element).isAll();
            Keyed[] values = TagModule.getKeyed(element);
            if (values == null || values.length == 0) {
                return false;
            }
            Class valueClass = values[0].getClass();
            for (Tag tag : tags) {
                if (!((Keyed)tag.getValues().iterator().next()).getClass().isAssignableFrom(valueClass)) {
                    return false;
                }
                if (this.isTagged((Tag<Keyed>)tag, values, !isAny)) {
                    if (and) continue;
                    return true;
                }
                if (!and) continue;
                return false;
            }
            return and;
        }, this.isNegated());
    }

    private boolean isTagged(Tag<Keyed> tag, Keyed @NotNull [] values, boolean allTagged) {
        for (Keyed value : values) {
            if (tag.isTagged(value)) {
                if (allTagged) continue;
                return true;
            }
            if (!allTagged) continue;
            return false;
        }
        return allTagged;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, this.elements, " tagged as " + this.tags.toString(event, debug));
    }
}

